package edu.rogachova.server.managers;

import edu.rogachova.common.DataManager;
import edu.rogachova.common.model.Worker;
import edu.rogachova.common.net.CommandResult;
import edu.rogachova.common.net.Request;
import edu.rogachova.common.net.ResultStatus;


import java.rmi.AccessException;
import java.time.*;
import java.util.*;

public class CollectionManager extends DataManager
{

    public HashMap<Long, Worker> employees = new HashMap<>();
    private String type;
    private Date initDate;

    protected Integer nextId;
    protected long nextKey;
    private FileManager fileManager;
    private DatabaseManager databaseManager;
    private ArrayList<Worker> workers;

    public CollectionManager(FileManager fileManager) throws AccessException{
        this.fileManager = fileManager;
        employees = fileManager.readFile();
        nextKey = getNextKey();
        setNextId();
        initDate = new Date();
        type = employees.getClass().getSimpleName();
    }

    public CollectionManager(DatabaseManager databaseManager){
        this.databaseManager = databaseManager;
        loadCollectionDB();
        initDate = new Date();
    }

    private long getNextKey(){
        return (long)(employees.size()+1);
    }

    private void setNextId(){
        List<Integer> ids = new ArrayList<>();
        for (Map.Entry<Long, Worker> w : employees.entrySet()) {
            ids.add(w.getValue().getId());
        }

        this.nextId = Collections.max(ids) + 1;
    }

    public Integer getNextId(){
        return nextId++;
    }

    public Date getInitDate(){
        return initDate;
    }

    public String getCollType(){
        return type;
    }

    public int getSize(){
        return employees.size();
    }


    public void loadCollectionDB(){
        workers = databaseManager.getAllWorkers();
        employees.clear();
        for(Worker w : workers){
            employees.put(w.getId().longValue(), w);
        }
    }

    @Override
    public void save(){
        fileManager.writeToFile(employees);
    }

    @Override
    public synchronized CommandResult insert(Request<?> request){
        Worker worker;
        try{
            worker = (Worker) request.entity;
        }
        catch (Exception exc){
            return new CommandResult(ResultStatus.ERROR, "В контроллер передан аргумент другого типа");
        }
        databaseManager.addWorker(worker, request.user);
        loadCollectionDB();
        return new CommandResult(ResultStatus.OK, "Новый элемент добавлен");
    }

    public synchronized Worker getById(long id) throws Exception
    {
        Long l = id;
        return databaseManager.getWorkerById(l.intValue());
    }

    @Override
    public synchronized CommandResult update(Request<?> request){
        Worker newWorker;
        try{
            newWorker = (Worker) request.entity;
        }
        catch (Exception exc){
            return new CommandResult(ResultStatus.ERROR, "В контроллер передан аргумент другого типа");
        }

        if(!databaseManager.belongsToUser(request.user, newWorker.getId())){
            return new CommandResult(ResultStatus.ERROR,
                    "Элемента, принадлежащего этому пользователю, с таким индексом не существует!");
        }

        try{
            databaseManager.updateWorker(request.user, newWorker);
            loadCollectionDB();
            return new CommandResult(ResultStatus.OK, "Изменения добавлены");
        }catch(Exception exception) {
            return new CommandResult(ResultStatus.ERROR, "Не удалось внести изменения");
        }
    }

    @Override
    public synchronized CommandResult clear(Request<?> request){
        databaseManager.deleteAllWorkers(request.user);
        employees.clear();
        return new CommandResult(ResultStatus.OK, "Все элементы из коллекции удалены");
    }


    @Override
    public synchronized CommandResult removeByKey(Request<?> request){
        try{
            Long id = (Long)request.entity;
            try
            {
                databaseManager.deleteWorker(request.user, id);
                loadCollectionDB();
            }catch(Exception e){

                return new CommandResult(ResultStatus.ERROR, e.getMessage());
            }
            return new CommandResult(ResultStatus.OK, "Элемент удален из коллекции");
        }catch(Exception exception) {
            return new CommandResult(ResultStatus.ERROR, "Передан аргумент другого типа");
        }
    }

    @Override
    public synchronized CommandResult show(Request<?> request){
        if(employees.size() == 0){
            return new CommandResult(ResultStatus.OK, "В коллекции нет элементов");
        }
        else{
            loadCollectionDB();
            StringBuffer message = new StringBuffer();
            for(long key : employees.keySet()){
                Worker w = employees.get(key);
                message.append(w.toString() + "\n");
            }
            return new CommandResult(ResultStatus.OK, message.toString());
        }
    }

    @Override
    public synchronized CommandResult removeGreater(Request<?> request){
        Worker worker;
        try{
            worker = (Worker)request.entity;
            HashMap <Long, Worker> W = new HashMap<>();
            int count = 0;
            for (Map.Entry<Long, Worker> w : employees.entrySet()) {
                if(w.getValue().compareTo(worker) > 0){
                    employees.remove(w.getKey());
                    databaseManager.deleteWorker(request.user, w.getKey());
                    count += 1;
                }
            }
            return new CommandResult(ResultStatus.OK, "Удалено "+count+" объектов");
        }catch(Exception exception) {
            return new CommandResult(ResultStatus.ERROR, "Передан аргумент другого типа");
        }
    }

    @Override
    public synchronized CommandResult removeGreaterKey(Request<?> request){
        try
        {
            Long usersKey = (Long) request.entity;
            ArrayList<Long> keys = new ArrayList<Long>();
            for (long collKey : employees.keySet())
            {
                keys.add(collKey);
            }

            int count = 0;
            for (long key : keys)
            {
                Worker w = employees.get(key);
                if (w.getId() > usersKey)
                {
                    databaseManager.deleteWorker(request.user, key);
                    employees.remove(key);
                    count++;
                }
            }


            return new CommandResult(ResultStatus.OK, String.format("Из коллекции успешно удалено %d элементов.", count));
        }catch(Exception exception) {
            return new CommandResult(ResultStatus.ERROR, "Передан аргумент другого типа");
        }
    }

    @Override
    public synchronized CommandResult info(Request<?> request){
        String type = "HashMap<Long, Route>";

        return new CommandResult(ResultStatus.OK,
                "Информация о коллекции: " + "\n" +
                        "Тип коллекции: " + type + "\n" +
                        "Дата инициализации: " + getInitDate() + "\n" +
                        "Количество элементов в коллекции: " + getSize()
        );
    }

    public synchronized CommandResult replaceLowerKey(Request<?> request)
    {
        try{
            Worker worker = (Worker)request.entity;
            Integer key = worker.getId();
            Worker workerToCompare = databaseManager.getWorkerById(key);
            if (worker.compareTo(workerToCompare) < 0)
            {
                employees.put(key.longValue(), worker);
                databaseManager.replaceWorker(request.user, worker);
                return new CommandResult(ResultStatus.OK, "Работник заменен");
            }
            loadCollectionDB();
            return new CommandResult(ResultStatus.OK, "Работник заменен");
        } catch (Exception e)
        {
            return new CommandResult(ResultStatus.ERROR, "Передан аргумент другого типа");
        }
    }

    @Override
    public synchronized CommandResult countLessSalary(Request<?> request){
        try
        {
            int userS = (Integer) request.entity;
            int count = 0;
            for (Map.Entry<Long, Worker> w : employees.entrySet())
            {
                if (w.getValue().getSalary() < userS)
                {
                    count += 1;
                }
            }
            return new CommandResult(ResultStatus.OK, "Работников с меньшей зарплатой - " + count);
        }catch(Exception exception) {
            return new CommandResult(ResultStatus.ERROR, "Передан аргумент другого типа");
        }
    }

    @Override
    public synchronized CommandResult filterStartWith(Request<?> request){
        try{
            String name = (String)request.entity;
            StringBuffer message = new StringBuffer();
            for(long key : employees.keySet()){
                Worker w = employees.get(key);
                if(w.getName().startsWith(name)){
                    message.append(w.toString() + "\n");
                }
            }
            return new CommandResult(ResultStatus.OK, message.toString());
        }catch(Exception exception) {
            return new CommandResult(ResultStatus.ERROR, "Передан аргумент другого типа");
        }
    }

    @Override
    public synchronized CommandResult printDSCEnd(Request<?> request){
        ArrayList<ZonedDateTime> arr = new ArrayList<>();
        for(long key : employees.keySet()){
            if(employees.get(key).getEndDate()!= null){
                ZonedDateTime endDate = employees.get(key).getEndDate();
                arr.add(endDate);
            }
        }

        Collections.sort(arr, Collections.reverseOrder());
        StringBuffer message = new StringBuffer();
        for(ZonedDateTime e :arr){
            message.append(e + "\n");
        }
        return new CommandResult(ResultStatus.OK, message.toString());
    }

}
