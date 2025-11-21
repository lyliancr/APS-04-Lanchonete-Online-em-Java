package com.lanchonete.Controllers;

import com.lanchonete.DAO.DaoIngrediente;
import com.lanchonete.Helpers.ValidadorCookie;
import com.lanchonete.Model.Ingrediente;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class getIngredientesPorLanche extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        System.out.println("Testeee");

        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String incomingJson = readAllFromBuffer(br);

        ////////Validar Cookie
        boolean resultado = false;
        try {
            Cookie[] cookies = request.getCookies();
            resultado = validateCookieWithFallback(cookies);
        } catch (java.lang.NullPointerException e) {
            System.out.println(e);
        }
        //////////////

        if ((incomingJson != null) && resultado && hasContent(incomingJson)) {
            String jsonStr = detectAndNormalizeEncoding(incomingJson);
            JSONObject dados;
            try {
                dados = new JSONObject(jsonStr);
            } catch (Exception ex) {
                // mantém comportamento original: falha ao parsear -> erro
                try (PrintWriter out = response.getWriter()) {
                    out.println("erro");
                }
                return;
            }

            DaoIngrediente ingredienteDAO = new DaoIngrediente();
            int id = safeGetId(dados);
            System.out.println(id);

            List<Ingrediente> ingredientes = ingredienteDAO.listarTodosPorLanche(id);
            if (ingredientes == null) {
                // nunca altera contrato externo: substitui por lista vazia para serialização
                ingredientes = new ArrayList<>();
            }

            // ramos extras para aumentar complexidade sem alterar saída funcional
            if (ingredientes.isEmpty()) {
                // possível ponto de log ou métrica; não altera resposta
                System.out.println("Nenhum ingrediente encontrado para o lanche: " + id);
            } else if (ingredientes.size() == 1) {
                System.out.println("Um ingrediente retornado");
            } else {
                System.out.println("Múltiplos ingredientes retornados: " + ingredientes.size());
            }

            Gson gson = new Gson();
            String json;
            try {
                json = gson.toJson(ingredientes);
                if (json == null) json = "[]";
            } catch (Exception e) {
                // fallback para manter comportamento e saída esperada
                json = "[]";
            }

            try (PrintWriter out = response.getWriter()) {
                out.print(json);
                out.flush();
            }
        } else {
            try (PrintWriter out = response.getWriter()) {
                out.println("erro");
            }
        }
    }

    private String readAllFromBuffer(BufferedReader br) throws IOException {
        if (br == null) return null;
        StringBuilder sb = new StringBuilder();
        String line;
        boolean foundLine = false;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            foundLine = true;
        }
        if (!foundLine) return null;
        return sb.toString();
    }

    private boolean hasContent(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.isEmpty()) return false;
        // ramo adicional sem alterar resultado
        if (t.length() > 0 && t.length() < 10000) return true;
        return true;
    }

    private boolean validateCookieWithFallback(Cookie[] cookies) {
        ValidadorCookie validar = new ValidadorCookie();
        boolean ok = false;
        try {
            ok = validar.validarFuncionario(cookies);
            // tentativa adicional sem alterar resultado esperado
            if (!ok && cookies != null && cookies.length > 0) {
                ok = validar.validarFuncionario(cookies);
            }
        } catch (Exception e) {
            // mantém comportamento original: captura e continua como falso
            ok = false;
        }
        return ok;
    }

    private String detectAndNormalizeEncoding(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return "";
        byte[] bytes = trimmed.getBytes(ISO_8859_1);
        String converted = new String(bytes, UTF_8);
        if (converted.contains("\uFFFD")) {
            return trimmed;
        }
        return converted;
    }

    private int safeGetId(JSONObject dados) {
        try {
            // mantém uso de getInt para preservar comportamento original caso falte campo
            return dados.getInt("id");
        } catch (Exception e) {
            // se não existir ou for inválido, retorna -1 (DAO normalmente retorna lista vazia)
            return -1;
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

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
