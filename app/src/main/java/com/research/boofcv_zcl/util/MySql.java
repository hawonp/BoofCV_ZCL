package com.research.boofcv_zcl.util;

import android.os.StrictMode;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.ArrayList;

public class MySql {

    private final String URL = "jdbc:mysql://rapa.chjitxu4bc4m.ap-northeast-2.rds.amazonaws.com";
    private final String USER = "admin";
    private final String PASSWORD = "raparapa";
    private final String PORT = "3306";
    private static final String DATABASE = "rapa";

    private Properties properties;
    private ArrayList<Data> db;

    public MySql()  {
        properties = new Properties();
        properties.put("user", USER);
        properties.put("password", PASSWORD);
        properties.put("port", PORT);
    }

    // TODO Change implementation as making a connecting for everytime you detect a cube is slowing the UI thread down
    public Data getData(String cube_id){
        Data result = null;
        try {
            Log.d("OpenCV", String.valueOf(properties.toString()));
            Log.d("OpenCV", String.valueOf(URL));
            //Temporary fix
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            Connection connection = DriverManager.getConnection(URL, properties);

            String sql = "SELECT rapa.cabinet.* FROM rapa.cabinet WHERE rapa.cabinet.id =?;";

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, String.valueOf(cube_id));

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                // retrieve data from database
                int id = Integer.parseInt(resultSet.getString("id"));
                int aruco_id = Integer.parseInt(resultSet.getString("aruco_id"));
                String model_number = resultSet.getString("model_number");
                String model_name = resultSet.getString("model_name");
                int residual_amount = Integer.parseInt(resultSet.getString("residual_amount"));
                int residual_percentage = Integer.parseInt(resultSet.getString("residual_percentage"));
//                Date last_order_data = resultSet.getDate("last_order_date");
//                Date expected_order_data = resultSet.getDate("expected_order_date");
                result = new Data(id, aruco_id, model_number, model_name, residual_amount, residual_percentage);
            }

            connection.close();

        } catch (Exception e) {
            Log.e("OpenCV", "Error Connecting to DB", e);
        }

        return result;
    }

}
