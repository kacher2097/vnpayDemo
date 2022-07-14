package com.payment.paymentcore.DAO;

public class SQLQuery {
    public static String addRequestQuery(){
        return  "Insert into payment(api_id, token_key, mobile, bank_code, account_no, pay_date, " +
                "addtional_data, debit_amount, resp_code, resp_desc, trace_transfer, message_type, " +
                "check_sum, order_code, user_name, real_amount, promotion_code, pay_method, pay_method_mms) " +
                "VALUES (?, ?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, ?, ?)";
    }
}
