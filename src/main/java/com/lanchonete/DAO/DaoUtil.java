/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lanchonete.DAO;

import java.sql.Connection;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

public class DaoUtil {
    private static DataSource ds;

    static {
        try {
            Context initContext = new InitialContext();
            ds = (DataSource) initContext.lookup("java:comp/env/jdbc/lanchonete");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao configurar DataSource", e);
        }
    }

    public static Connection conecta() {
        try {
            return ds.getConnection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
