package com.payment.paymentcore.DAO;

import com.payment.paymentcore.config.HikariCPResource;
import com.payment.paymentcore.model.PaymentRequest;
import com.payment.paymentcore.service.RabbitMQService;
import com.payment.paymentcore.util.ErrorCode;
import com.payment.paymentcore.util.PaymentException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PaymentDAO {
    private static final Logger log = LogManager.getLogger(RabbitMQService.class);
    private static PaymentDAO instance;

    public PaymentDAO() {
    }

    public static PaymentDAO getInstance() {
        if (instance == null) {
            instance = new PaymentDAO();
        }
        return instance;
    }

    public boolean addPaymentRequest(PaymentRequest paymentRequest) {
        log.info("Begin add payment request into database with data: {}", paymentRequest);
        String sqlAdd = SQLQuery.addRequestQuery();
        Connection conn = null;
        PreparedStatement prepareStatement = null;
        try {
            conn = HikariCPResource.getConnection();
            conn.setAutoCommit(false);
            prepareStatement = conn.prepareStatement(sqlAdd);

            prepareStatement.setString(1, paymentRequest.getApiID());
            prepareStatement.setString(2, paymentRequest.getTokenKey());
            prepareStatement.setString(3, paymentRequest.getMobile());
            prepareStatement.setString(4, paymentRequest.getBankCode());
            prepareStatement.setString(5, paymentRequest.getAccountNo());
            prepareStatement.setString(6, paymentRequest.getPayDate());

            prepareStatement.setString(7, paymentRequest.getAdditionalData());
            prepareStatement.setDouble(8, paymentRequest.getDebitAmount());
            prepareStatement.setString(9, paymentRequest.getRespCode());
            prepareStatement.setString(10, paymentRequest.getRespDesc());
            prepareStatement.setString(11, paymentRequest.getTraceTransfer());
            prepareStatement.setString(12, paymentRequest.getMessageType());

            prepareStatement.setString(13, paymentRequest.getCheckSum());
            prepareStatement.setString(14, paymentRequest.getOrderCode());
            prepareStatement.setString(15, paymentRequest.getUserName());
            prepareStatement.setDouble(16, paymentRequest.getRealAmount());
            prepareStatement.setString(17, paymentRequest.getPromotionCode());
            prepareStatement.setString(18, paymentRequest.getAddValue().getPayMethod());
            prepareStatement.setInt(19, paymentRequest.getAddValue().getPayMethodMMS());

            prepareStatement.executeUpdate();
            conn.commit();
            log.info("Execute query success");
            return true;
        } catch (ExceptionInInitializerError e) {
            log.error("Insert into DB has exception: {}", e);
            throw new PaymentException(ErrorCode.CONNECT_DB_FAIL);
        } catch (SQLException e) {
            log.error("SQL exception ", e);
            throw new PaymentException(ErrorCode.SQL_EXCEPTION);
        } finally {
            try {
                if (prepareStatement != null) {
                    prepareStatement.close();
                }
            } catch (SQLException e) {
                log.error("Error when close prepared statement {}", e);
            } finally {
                try {
                    if(conn != null){
                        conn.close();
                    }
                } catch (SQLException e) {
                    log.error("Error when close connection {}", e);
                }
            }


        }
    }

}
