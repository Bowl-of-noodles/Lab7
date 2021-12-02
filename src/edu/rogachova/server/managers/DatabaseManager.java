package edu.rogachova.server.managers;

import edu.rogachova.common.model.*;
import edu.rogachova.common.net.CommandResult;
import edu.rogachova.common.net.Request;
import edu.rogachova.common.net.ResultStatus;

import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;

public class DatabaseManager
{
    private final String url;
    private final String username;
    private final String password;
    private Connection dbConnection;

    public DatabaseManager(String url, String username, String password){
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public void connectToDatabase(){
        try {
            dbConnection = DriverManager.getConnection(url, username, password);
            System.out.println("Подключение к базе данных установлено.");

            /*for testing
            Statement statement = dbConnection.createStatement();
            ResultSet rs = statement.executeQuery(cr);
            if(rs.next())
            {
                System.out.println(rs.getString(1));
            }*/

        } catch (SQLException e) {
            System.out.println("Не удалось выполнить подключение к базе данных.");
            e.printStackTrace();
            System.exit(-1);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    String cr ="SELECT username FROM WorkerOwners";

    public CommandResult login(Request<?> request){
        try {
            User user = (User) request.entity;
            if (validateUser(user))
                return new CommandResult(ResultStatus.OK,
                        String.format("Добро пожаловать, %s!", user.getUsername()));
            return new CommandResult(ResultStatus.ERROR, "Неверный логин или пароль.");
        } catch (SQLException exception) {
            exception.printStackTrace();
            return new CommandResult(ResultStatus.ERROR, "SQL-ошибка на сервере");
        } catch (Exception exception) {
            return new CommandResult(ResultStatus.ERROR, "Передан аргумент другого типа");
        }
    }

    private boolean validateUser(User user) throws SQLException{
        String validate_user = String.format("SELECT COUNT(*) FROM %s WHERE %s = ? AND %s = ?",
                "WorkerOwners", "username", "\"password\"");
        PreparedStatement statement = dbConnection.prepareStatement(validate_user);
        statement.setString(1, user.getUsername());
        statement.setString(2, user.getHashedPassword());
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();
        int count = resultSet.getInt(1);
        statement.close();
        return count != 0;
    }

    public CommandResult register(Request<?> request){
        try {
            User user = (User) request.entity;
            if (!userExists(user.getUsername())) {
                String add_user = String.format("INSERT INTO %s (%s, %s) VALUES (?, ?)",
                        "WorkerOwners", "username", "\"password\"");
                PreparedStatement statement = dbConnection.prepareStatement(add_user);
                statement.setString(1, user.getUsername());
                statement.setString(2, user.getHashedPassword());
                statement.executeUpdate();
                statement.close();
                return new CommandResult(ResultStatus.OK,
                        String.format("Добро пожаловать, %s!", user.getUsername()));
            }
            return new CommandResult(ResultStatus.ERROR, "Пользователь с таким именем уже зарегистрирован.");
        } catch (SQLException exception) {
            exception.printStackTrace();
            return new CommandResult(ResultStatus.ERROR, "SQL-ошибка на сервере");
        } catch (Exception exception) {
            return new CommandResult(ResultStatus.ERROR, "Передан аргумент другого типа");
        }
    }

    private boolean userExists(String username) throws SQLException {
        String check_user =  String.format("SELECT COUNT(*) FROM %s WHERE %s = ?",
                "WorkerOwners", "username");
        PreparedStatement statement = dbConnection.prepareStatement(check_user);
        statement.setString(1, username);
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();
        int count = resultSet.getInt(1);
        statement.close();
        return count != 0;
    }

    public synchronized ArrayList<Worker> getAllWorkers() {
        final String q = "SELECT * FROM \"workers\";";

        ArrayList<Worker> arr = new ArrayList<>();
        try {
            PreparedStatement getStatement = dbConnection.prepareStatement(q);
            ResultSet result = getStatement.executeQuery();

            while (result.next()) {
                try {
                    Worker newWorker = new Worker();

                    newWorker.setId(result.getInt(1));
                    newWorker.setUser(result.getString(2));
                    newWorker.setName(result.getString(3));

                    Coordinates coord = new Coordinates();
                    coord.setX(result.getFloat(4));
                    coord.setY(result.getInt(5));

                    newWorker.setCoordinates(coord);
                    ZonedDateTime creationDate = ZonedDateTime.ofInstant(result.getTimestamp(6).toInstant(), ZoneId.of("UTC"));
                    newWorker.setCreationDate(creationDate);

                    newWorker.setSalary(result.getInt(7));

                    ZonedDateTime endDate = ZonedDateTime.ofInstant(result.getTimestamp(8).toInstant(), ZoneId.of("UTC"));
                    newWorker.setEndDate(endDate);

                    String position = result.getString(9);
                    if(position == null) {newWorker.setPosition(null);}
                    else{ newWorker.setPosition(Position.valueOf(position));}

                    String status = result.getString(10);
                    if(status == null) {newWorker.setStatus(null);}
                    else{newWorker.setStatus(Status.valueOf(status));}

                    Organization org = new Organization();
                    if (result.getString(11) != null)
                    {
                        org.setFullName(result.getString(11));

                        String type = result.getString(12);
                        if (status == null) {
                            org.setType(null);
                        } else {
                            org.setType(OrganizationType.valueOf(type));
                        }

                        Address address = new Address();
                        address.setStreet(result.getString(13));

                        Location loc = new Location();
                        loc.setX(result.getLong(14));
                        loc.setY(result.getInt(15));
                        loc.setZ(result.getLong(16));
                        loc.setName(result.getString(17));

                        address.setTown(loc);
                        org.setPostalAddress(address);
                        newWorker.setOrganization(org);
                    }else{
                        newWorker.setOrganization(new Organization(new Address(new Location())));
                    }
                    arr.add(newWorker);
                } catch (Exception exc) {
                    throw new Exception("Невозможно загрузить данные из базы: они повреждены");
                }
            }
            getStatement.close();
        }catch(Exception e){
            System.out.println("Произошла ошибка при загрузке данных из БД");
        }
        return arr;
    }

    public synchronized Worker getWorkerById(Integer workerId) throws Exception {
        final String q = "SELECT * FROM workers WHERE idW = ?;";

        try {
            PreparedStatement getStatement = dbConnection.prepareStatement(q);
            getStatement.setLong(1, workerId);
            ResultSet result = getStatement.executeQuery();

            if(result.next()){
                try{
                    Worker newWorker = new Worker();

                    newWorker.setId(result.getInt(1));
                    newWorker.setUser(result.getString(2));
                    newWorker.setName(result.getString(3));

                    Coordinates coord = new Coordinates();
                    coord.setX(result.getFloat(4));
                    coord.setY(result.getInt(5));

                    newWorker.setCoordinates(coord);
                    ZonedDateTime creationDate = ZonedDateTime.ofInstant(result.getTimestamp(6).toInstant(), ZoneId.of("UTC"));
                    newWorker.setCreationDate(creationDate);

                    newWorker.setSalary(result.getInt(7));

                    ZonedDateTime endDate = ZonedDateTime.ofInstant(result.getTimestamp(8).toInstant(), ZoneId.of("UTC"));
                    newWorker.setEndDate(endDate);

                    String position = result.getString(9);
                    if(position == null) {newWorker.setPosition(null);}
                    else{ newWorker.setPosition(Position.valueOf(position));}

                    String status = result.getString(10);
                    if(status == null) {newWorker.setStatus(null);}
                    else{newWorker.setStatus(Status.valueOf(status));}

                    Organization org = new Organization();
                    if (result.getString(11) != null) {
                        org.setFullName(result.getString(11));

                        String type = result.getString(12);
                        if (status == null)
                        {
                            org.setType(null);
                        } else
                        {
                            org.setType(OrganizationType.valueOf(type));
                        }

                        Address address = new Address();
                        address.setStreet(result.getString(13));

                        Location loc = new Location();
                        loc.setX(result.getLong(14));
                        loc.setY(result.getInt(15));
                        loc.setZ(result.getLong(16));
                        loc.setName(result.getString(17));

                        address.setTown(loc);
                        org.setPostalAddress(address);
                        newWorker.setOrganization(org);
                    }
                    else{
                        newWorker.setOrganization(new Organization(new Address(new Location())));
                    }
                    return newWorker;
                }
                catch (Exception exc){
                    throw new Exception("Невозможно загрузить данные из базы: они повреждены");
                }
            }
            getStatement.close();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        throw new Exception("Маршрута с таким идентификатором не сеществует");
    }

    public synchronized void addWorker(Worker w, String ownerName) {
        final String q = "INSERT INTO workers (ownerName, nameW, x, y, creationDate, salary, endDate, positionW, " +
                "status, orgName, orgType, street, townX, townY, townZ, townName) VALUES " +
                "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        try {
            PreparedStatement addStatement = dbConnection.prepareStatement(q);
            addStatement.setString(1, ownerName);
            addStatement.setString(2, w.getName());
            addStatement.setDouble(3, w.getCoordinates().getX());
            addStatement.setDouble(4, w.getCoordinates().getY());
            addStatement.setTimestamp(5, Timestamp.from(w.getCreationDate().toInstant()));
            addStatement.setInt(6, w.getSalary());
            addStatement.setTimestamp(7, Timestamp.from(w.getEndDate().toInstant()));
            String position = w.getPosition() != null ? w.getPosition().toString() : null;
            addStatement.setString(8, position);
            String status = w.getStatus() != null ? w.getStatus().toString() : null;
            addStatement.setString(9, status);
            addStatement.setString(10, w.getOrganization().getFullName());
            String type = w.getOrganization().getType()!= null ? w.getOrganization().getType().name() : null;
            addStatement.setString(11, type);
            addStatement.setString(12, w.getOrganization().getPostalAddress().getStreet());
            addStatement.setLong(13, w.getOrganization().getPostalAddress().getTown().getX());
            addStatement.setInt(14, w.getOrganization().getPostalAddress().getTown().getY());
            addStatement.setLong(15, w.getOrganization().getPostalAddress().getTown().getZ());
            addStatement.setString(16, w.getOrganization().getPostalAddress().getTown().getName());

            addStatement.executeUpdate();
            addStatement.close();

        }catch(SQLException e){
            System.out.println("Произошла ошибка при внесении нового элемента в БД");
        }
    }

    public synchronized void deleteWorker(String username, Long workerId) {
        final String q = "DELETE FROM workers WHERE ownerName = ? AND idW = ?;";
        try {
            PreparedStatement deleteStatement = dbConnection.prepareStatement(q);
            deleteStatement.setString(1, username);
            deleteStatement.setLong(2, workerId);
            int rows = deleteStatement.executeUpdate();
            deleteStatement.close();
            if (rows != 1) {
                throw new SQLException(String.format("Работник с таким id = %d, не пренадлежит данному пользователю или не существует", workerId));
            }
        }catch (SQLException e){
            System.out.println("Работник с таким id = "+ workerId+", не пренадлежит данному пользователю или не существует");
        }
    }

    public synchronized void deleteAllWorkers(String username) {
        final String q = "DELETE FROM workers WHERE ownerName = ?;";
        try {
            PreparedStatement deleteStatement = dbConnection.prepareStatement(q);
            deleteStatement.setString(1, username);
            deleteStatement.executeUpdate();
            deleteStatement.close();
        }catch (SQLException e){
            System.out.println("Произошла ошибка при удалении данных");
        }

    }

    public synchronized void updateWorker(String ownerName, Worker w){
        final String q = "UPDATE workers SET nameW = ?, x = ?, y = ?, creationDate = ?, salary = ?, endDate = ?, positionW = ?," +
                "status = ?, orgName = ?, orgType = ?, street = ?, townX = ?, townY = ?, townZ = ?, townName = ? WHERE ownerName = ? AND idW = ?;";

        try {
            PreparedStatement addStatement = dbConnection.prepareStatement(q);
            Worker existedWorker = getWorkerById(w.getId());

            String name = w.getName() != null ? w.getName() : existedWorker.getName();
            addStatement.setString(1, name);

            addStatement.setDouble(2, existedWorker.getCoordinates().getX());

            addStatement.setDouble(3, existedWorker.getCoordinates().getY());

            addStatement.setTimestamp(4, Timestamp.from(w.getCreationDate().toInstant()));

            Integer salary = w.getSalary() == 0 ? existedWorker.getSalary() : w.getSalary();
            addStatement.setInt(5, salary);

            addStatement.setTimestamp(6, Timestamp.from(existedWorker.getEndDate().toInstant()));

            String position = w.getPosition() != null ? w.getPosition().toString() : existedWorker.getPosition().name();
            addStatement.setString(7, position);

            String status = w.getStatus() != null ? w.getStatus().toString() : existedWorker.getStatus().toString();
            addStatement.setString(8, status);

            if(w.getOrganization() != null)
            {
                addStatement.setString(9, existedWorker.getOrganization().getFullName());

                String type;
                if(existedWorker.getOrganization().getType()!=null){
                    type = w.getOrganization().getType() != null ? w.getOrganization().getType().name() : existedWorker.getOrganization().getType().name();
                }else{
                    type = w.getOrganization().getType() != null ? w.getOrganization().getType().name() : null;
                }
                addStatement.setString(10, type);

                addStatement.setString(11, existedWorker.getOrganization().getPostalAddress().getStreet());

                addStatement.setLong(12, existedWorker.getOrganization().getPostalAddress().getTown().getX());

                addStatement.setInt(13, existedWorker.getOrganization().getPostalAddress().getTown().getY());

                addStatement.setLong(14, existedWorker.getOrganization().getPostalAddress().getTown().getZ());

                addStatement.setString(15, existedWorker.getOrganization().getPostalAddress().getTown().getName());
            }else{
                if(existedWorker.getOrganization() != null){
                    addStatement.setString(9,existedWorker.getOrganization().getFullName());
                    String type = existedWorker.getOrganization().getType()!= null ? existedWorker.getOrganization().getType().name() : null;
                    addStatement.setString(10, type);
                    addStatement.setString(11, existedWorker.getOrganization().getPostalAddress().getStreet());
                    addStatement.setLong(12, existedWorker.getOrganization().getPostalAddress().getTown().getX());
                    addStatement.setInt(13,existedWorker.getOrganization().getPostalAddress().getTown().getY());
                    addStatement.setLong(14,existedWorker.getOrganization().getPostalAddress().getTown().getZ());
                    addStatement.setString(15,existedWorker.getOrganization().getPostalAddress().getTown().getName());
                }else{
                    addStatement.setString(9,null);
                    addStatement.setString(10, null);
                    addStatement.setString(11, null);
                    addStatement.setLong(12, 0);
                    addStatement.setInt(13,0);
                    addStatement.setLong(14,0);
                    addStatement.setString(15,null);
                }
            }
            addStatement.setString(16, ownerName);
            addStatement.setInt(17, w.getId());

            addStatement.executeUpdate();
            addStatement.close();
        }catch(SQLException e){
            System.out.println("Произошла ошибка при внесении нового элемента в БД");
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public synchronized void replaceWorker(String ownerName, Worker w){
        final String q = "UPDATE workers SET nameW = ?, x = ?, y = ?, creationDate = ?, salary = ?, endDate = ?, positionW = ?," +
                "status = ?, orgName = ?, orgType = ?, street = ?, townX = ?, townY = ?, townZ = ?, townName = ? WHERE ownerName = ? AND idW = ?;";

        try
        {
            PreparedStatement addStatement = dbConnection.prepareStatement(q);

            addStatement.setString(1, w.getName());
            addStatement.setDouble(2, w.getCoordinates().getX());
            addStatement.setDouble(3, w.getCoordinates().getY());
            addStatement.setTimestamp(4, Timestamp.from(w.getCreationDate().toInstant()));
            addStatement.setInt(5, w.getSalary());
            addStatement.setTimestamp(6, Timestamp.from(w.getEndDate().toInstant()));

            String position = w.getPosition() != null ? w.getPosition().toString() : null;
            addStatement.setString(7, position);

            String status = w.getStatus() != null ? w.getStatus().toString() : null;
            addStatement.setString(8, status);
            if(w.getOrganization() != null) {
                addStatement.setString(9, w.getOrganization().getFullName());

                String type = w.getOrganization().getType()!= null ? w.getOrganization().getType().name() : null;
                addStatement.setString(10, type);

                addStatement.setString(11, w.getOrganization().getPostalAddress().getStreet());

                addStatement.setLong(12, w.getOrganization().getPostalAddress().getTown().getX());

                addStatement.setInt(13, w.getOrganization().getPostalAddress().getTown().getY());

                addStatement.setLong(14, w.getOrganization().getPostalAddress().getTown().getZ());

                addStatement.setString(15, w.getOrganization().getPostalAddress().getTown().getName());
            } else{
                addStatement.setString(9,null);
                addStatement.setString(10, null);
                addStatement.setString(11, null);
                addStatement.setLong(12, 0);
                addStatement.setInt(13,0);
                addStatement.setLong(14,0);
                addStatement.setString(15,null);
            }
            addStatement.setString(16, ownerName);
            addStatement.setInt(17, w.getId());

            addStatement.executeUpdate();
            addStatement.close();
        }catch(SQLException e){
            System.out.println("Произошла ошибка при замещении данных");
        }
    }

    public synchronized boolean belongsToUser(String username, long routeId) {
        final String q = "SELECT count(*) FROM workers WHERE ownerName = ? AND idW = ?;";
        int rows = 0;
        try {
            PreparedStatement countStatement = dbConnection.prepareStatement(q);
            countStatement.setString(1, username);
            countStatement.setLong(2, routeId);
            ResultSet rs = countStatement.executeQuery();
            rs.next();
            rows = rs.getInt(1);
            countStatement.close();
        }
        catch (Exception exc) {
            System.out.println("Произошла ошибка");
        }
        return rows != 0;
    }
}

