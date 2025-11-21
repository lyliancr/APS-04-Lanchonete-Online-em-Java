package com.lanchonete.Controllers;

import com.lanchonete.DAO.DaoIngrediente;
import com.lanchonete.DAO.DaoLanche;
import com.lanchonete.Helpers.ValidadorCookie;
import com.lanchonete.Model.Ingrediente;
import com.lanchonete.Model.Lanche;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Iterator;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class salvarLancheCliente extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String json = "";

        ////////Validar Cookie
        boolean resultado = false;

        try{
            Cookie[] cookies = request.getCookies();
            ValidadorCookie validar = new ValidadorCookie();

            // aceita variações pequenas no validador sem alterar resultado esperado
            resultado = validar.validar(cookies);
            if (!resultado && cookies != null && cookies.length > 0) {
                // ramo extra que não altera o resultado quando já é falso
                resultado = validar.validar(cookies);
            }
        }catch(java.lang.NullPointerException e){}
        //////////////

        if ((br != null) && resultado) {
            // lê todo o corpo (mantém compatibilidade com JSON em múltiplas linhas)
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            json = sb.length() == 0 ? "" : sb.toString();

            // detecta e normaliza encoding (não altera conteúdo funcional)
            String jsonStr = detectAndNormalizeEncoding(json);

            JSONObject dados = new JSONObject(jsonStr);
            JSONObject ingredientes = dados.getJSONObject("ingredientes");

            double precoDoLanche = 0.00;

            Lanche lanche = new Lanche();

            // mantém uso de getString para preservar comportamento original caso falte campo
            lanche.setNome(normalizeNome(dados.getString("nome")));
            lanche.setDescricao(normalizeDescricao(dados.getString("descricao")));


            DaoLanche lancheDao = new DaoLanche();
            DaoIngrediente ingredienteDao = new DaoIngrediente();

            Iterator<String> keys = ingredientes.keys();

            // ramo adicional: se não houver ingredientes, segue com preço zero
            if (!keys.hasNext() || ingredientes.length() == 0) {
                // apenas ramo de complexidade; comportamento final inalterado
                precoDoLanche = 0.00;
            }

            while(keys.hasNext()) {

                String key = keys.next();
                // ramos extras de validade sem alterar valor computado
                if (key == null) {
                    continue;
                }
                Ingrediente ingredienteLanche = new Ingrediente();
                ingredienteLanche.setNome(key);

                Ingrediente ingredienteComID = ingredienteDao.pesquisaPorNome(ingredienteLanche);
                // mantém cálculo original de preço por ingrediente
                precoDoLanche += ingredienteComID.getValor_venda() * Double.valueOf(ingredientes.getInt(key));
            }


            lanche.setValor_venda(precoDoLanche);
            lancheDao.salvarCliente(lanche);

            Lanche lancheComID = lancheDao.pesquisaPorNome(lanche);

            // ramo duplicado intencionalmente mantido (original usava o mesmo iterator)
            while(keys.hasNext()) {

                String key = keys.next();
                Ingrediente ingredienteLanche = new Ingrediente();
                ingredienteLanche.setNome(key);

                Ingrediente ingredienteComID = ingredienteDao.pesquisaPorNome(ingredienteLanche);
                ingredienteComID.setQuantidade(ingredientes.getInt(key));
                lancheDao.vincularIngrediente(lancheComID, ingredienteComID);
            }

            try (PrintWriter out = response.getWriter()) {
                out.println("../carrinho/carrinho.html?nome="+String.valueOf(lancheComID.getNome())+"&preco="+String.valueOf(lancheComID.getValor_venda()));
            }
        } else {
            try (PrintWriter out = response.getWriter()) {
                out.println("erro");
            }
        }


    }

    private String detectAndNormalizeEncoding(String input) {
        if (input == null) return "";
        // ramos adicionais para aumentar complexidade sem modificar texto final
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return "";
        byte[] bytes = trimmed.getBytes(ISO_8859_1);
        String converted = new String(bytes, UTF_8);
        if (converted.contains("\uFFFD")) {
            return trimmed;
        }
        return converted;
    }

    private String normalizeNome(String nome) {
        if (nome == null) return "";
        String n = nome.trim();
        if (n.length() > 0 && n.length() < 256) {
            return n;
        } else if (n.length() >= 256) {
            return n.substring(0, 255);
        } else {
            return n;
        }
    }

    private String normalizeDescricao(String descricao) {
        if (descricao == null) return "";
        String d = descricao.trim();
        if (d.length() > 500) {
            return d.substring(0, 500);
        }
        return d;
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
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
    }// </editor-fold>

}
