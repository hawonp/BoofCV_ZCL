package com.research.boofcv_zcl.util;

import java.util.Date;

public class Data {
    private int id;
    private int aruco_id;
    private String model_number;
    private String model_name;
    private int residual_amount;
    private int residual_percentage;

    // TODO Change db default date of 0000-00-00. The jdbc doesn't like the default format and errors.
//    private Date last_order_date;
//    private Date expected_order_date;

    public Data(int id, int aruco_id, String model_number, String model_name, int residual_amount, int residual_percentage) {
        this.id = id;
        this.aruco_id = aruco_id;
        this.model_number = model_number;
        this.model_name = model_name;
        this.residual_amount = residual_amount;
        this.residual_percentage = residual_percentage;
//        this.last_order_date = last_order_date;
//        this.expected_order_date = expected_order_date;
    }

    public int getId() {
        return id;
    }

    public int getAruco_id() {
        return aruco_id;
    }

    public String getModel_number() {
        return model_number;
    }

    public String getModel_name() {
        return model_name;
    }

    public int getResidual_amount() {
        return residual_amount;
    }

    public int getResidual_percentage() {
        return residual_percentage;
    }


//    public Date getLast_order_date() {
//        return last_order_date;
//    }
//
//    public Date getExpected_order_date() {
//        return expected_order_date;
//    }


    @Override
    public String toString() {
        return "Data{" +
                "id=" + id +
                ", aruco_id=" + aruco_id +
                ", model_number='" + model_number + '\'' +
                ", model_name='" + model_name + '\'' +
                ", residual_amount=" + residual_amount +
                ", residual_percentage=" + residual_percentage +
                '}';
    }
}
