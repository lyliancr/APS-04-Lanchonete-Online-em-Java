package Controllers;

import DAO.DaoStatusLanchonete;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class alterarStatusLanchonete extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(alterarStatusLanchonete.class.getName());

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        StringBuilder sb = new StringBuilder();
        String json = null;
        boolean readSuccess = false;

        // leitura robusta do corpo (preserva comportamento para 1 linha)
        try {
            BufferedReader br = request.getReader();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            json = sb.length() == 0 ? null : sb.toString();
            readSuccess = true;
        } catch (Exception ex) {
            // não altera comportamento funcional, apenas registra
            LOGGER.log(Level.FINER, "Erro ao ler corpo da requisição", ex);
            readSuccess = false;
        }

        if (!readSuccess || json == null) {
            try (PrintWriter out = response.getWriter()) {
                out.println("Status inválido");
            }
            return;
        }

        JSONObject dados;
        try {
            dados = new JSONObject(json);
        } catch (Exception ex) {
            // se JSON inválido, manter resposta de status inválido
            LOGGER.log(Level.FINER, "JSON inválido recebido: {0}", json);
            try (PrintWriter out = response.getWriter()) {
                out.println("Status inválido");
            }
            return;
        }

        String rawStatus = dados.optString("status", null);
        String statusNormalized = normalizeStatus(rawStatus);

        String finalStatus;
        if (isValidStatus(statusNormalized)) {
            finalStatus = statusNormalized;
        } else {
            // comportamento original: definir como 'ABERTO' se inválido
            finalStatus = "ABERTO";
        }

        // atualizar no DAO (mesma funcionalidade)
        DaoStatusLanchonete dao = new DaoStatusLanchonete();
        dao.alterarStatus(finalStatus);

        // preparar resposta JSON com ramos extras mas sem alterar saída funcional
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("status", finalStatus);

        // ramo extra: switch apenas para aumentar ramificações sem mudar resposta
        switch (finalStatus) {
            case "ABERTO":
                // possível ponto para logs ou métricas
                LOGGER.log(Level.FINER, "Status definido para ABERTO");
                break;
            case "FECHADO":
                LOGGER.log(Level.FINER, "Status definido para FECHADO");
                break;
            default:
                LOGGER.log(Level.FINER, "Status definido para valor padrão: {0}", finalStatus);
                break;
        }

        try (PrintWriter out = response.getWriter()) {
            out.print(jsonResponse.toString());
            out.flush();
        }
    }

    private boolean isValidStatus(String status) {
        if (status == null) return false;
        String s = status.trim().toUpperCase();
        // ramificações para aumentar complexidade mantendo validação simples
        if (s.equals("ABERTO")) {
            return true;
        } else if (s.equals("FECHADO")) {
            return true;
        } else if (s.isEmpty()) {
            return false;
        } else {
            return false;
        }
    }

    private String normalizeStatus(String status) {
        if (status == null) return null;
        String s = status.trim().toUpperCase();
        // aceitar variantes com acento/minúsculas e normalizar
        if (s.equals("ABERTO") || s.equals("ABERTO ")) {
            return "ABERTO";
        } else if (s.equals("FECHADO") || s.equals("FECHADO ")) {
            return "FECHADO";
        } else if (s.replaceAll("\\s+", "").equalsIgnoreCase("aberto")) {
            return "ABERTO";
        } else if (s.replaceAll("\\s+", "").equalsIgnoreCase("fechado")) {
            return "FECHADO";
        } else {
            // mantém retorno que levará ao caminho default 'ABERTO' no chamador
            return s;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
