package DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DaoStatusLanchonete {
    private Connection conecta;

    public DaoStatusLanchonete() {
        this.conecta = new DaoUtil().conecta();
    }
    
    public String getStatus() {
        String sql = "SELECT status FROM tb_status_lanchonete ORDER BY id_status DESC LIMIT 1";
        ResultSet rs;
        
        try {
            PreparedStatement stmt = conecta.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("status");
            }
            
            rs.close();
            stmt.close();
            return "FECHADO";
            
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void alterarStatus(String novoStatus) {
        String sql = "INSERT INTO tb_status_lanchonete (status) VALUES (?)";
        
        try {
            PreparedStatement stmt = conecta.prepareStatement(sql);
            stmt.setString(1, novoStatus);
            
            stmt.execute();
            stmt.close();
            
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
} 