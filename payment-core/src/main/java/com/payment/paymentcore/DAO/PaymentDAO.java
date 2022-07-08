package com.payment.paymentcore.DAO;

import com.payment.paymentcore.config.HikariCPResource;
import com.payment.paymentcore.model.PaymentRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {
    private static PaymentDAO instance;

    public static PaymentDAO getInstance() {
        if (instance == null) {
            instance = new PaymentDAO();
        }
        return instance;
    }

    public PaymentDAO() {
    }

    public boolean addPaymentRequest(PaymentRequest PaymentRequest) {
        int id = 0;
        Connection conn = null;
        PreparedStatement prepareStatement = null;
        try {
            String sqlAdd = "Insert into payment(api_id, token_key, mobile, bank_code, account_no, pay_date, " +
                    "addtional_data, debit_amount, resp_code, resp_desc, trace_transfer, message_type, " +
                    "check_sum, order_code, user_name, real_amount, promotion_code, pay_method, pay_method_mms) " +
                    "VALUES (?, ?, ?, ?, ?, ?, " +
                    "?, ?, ?, ?, ?, ?, " +
                    "?, ?, ?, ?, ?, ?, ?)";

            conn = HikariCPResource.getConnection();
            prepareStatement = conn.prepareStatement(sqlAdd, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setString(1, PaymentRequest.getApiID());
            prepareStatement.setString(2, PaymentRequest.getTokenKey());
            prepareStatement.setString(3, PaymentRequest.getMobile());
            prepareStatement.setString(4, PaymentRequest.getBankCode());
            prepareStatement.setString(5, PaymentRequest.getAccountNo());
            prepareStatement.setString(6, PaymentRequest.getPayDate());

            prepareStatement.setString(7, PaymentRequest.getAdditionalData());
            prepareStatement.setDouble(8, PaymentRequest.getDebitAmount());
            prepareStatement.setString(9, PaymentRequest.getRespCode());
            prepareStatement.setString(10, PaymentRequest.getRespDesc());
            prepareStatement.setString(11, PaymentRequest.getTraceTransfer());
            prepareStatement.setString(12, PaymentRequest.getMessageType());

            prepareStatement.setString(13, PaymentRequest.getCheckSum());
            prepareStatement.setString(14, PaymentRequest.getOrderCode());
            prepareStatement.setString(15, PaymentRequest.getUserName());
            prepareStatement.setDouble(16, PaymentRequest.getRealAmount());
            prepareStatement.setString(17, PaymentRequest.getPromotionCode());
            prepareStatement.setString(18, PaymentRequest.getAddValue().getPayMethod());
            prepareStatement.setInt(19, PaymentRequest.getAddValue().getPayMethodMMS());

            boolean resultAdd = prepareStatement.execute();
            if(!resultAdd){
                throw new SQLException("Insert into DB PaymentRequest failed");

            }else {

                return true;
            }

//            try (ResultSet rs = prepareStatement.getGeneratedKeys()) {
//                if (rs.next()) {
//                    PaymentRequest.getApiID(rs.getString(1));
//                } else {
//                    throw new SQLException("Creating PaymentRequest failed, no ID obtained.");
//                }
//            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
