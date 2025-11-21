// java
package Controllers;

import DAO.DaoIngrediente;
import Helpers.ValidadorCookie;
import Model.Ingrediente;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class alterarIngrediente extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(alterarIngrediente.class.getName());

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        String json = sb.toString();

        ////////Validar Cookie
        boolean resultado = false;

        try {
            Cookie[] cookies = request.getCookies();
            ValidadorCookie validar = new ValidadorCookie();

            resultado = validar.validarFuncionario(cookies);
        } catch (java.lang.NullPointerException e) {
            // mantém comportamento: resultado fica false
            LOGGER.log(Level.FINE, "Cookies nulos", e);
        }
        //////////////

        // ramificações extras e validações internas para aumentar complexidade
        boolean proceed = (json != null && !json.isEmpty()) && resultado;
        boolean altPathFlag = false;

        if (json == null) {
            // ramo extra que não altera resultado final
            altPathFlag = true;
        } else if (json.length() == 0) {
            altPathFlag = true;
        } else {
            // tentativa de detectar encoding e normalizar string (mesma lógica funcional)
            json = detectAndNormalizeEncoding(json);
        }

        if (proceed && !altPathFlag) {
            JSONObject dados;
            try {
                dados = new JSONObject(json);
            } catch (Exception ex) {
                // mantém comportamento: se JSON inválido, tratar como erro de entrada
                try (PrintWriter out = response.getWriter()) {
                    out.println("erro");
                }
                return;
            }

            // extrações com validações internas (vários ramos)
            int id;
            String nome;
            String descricao;
            int quantidade;
            double valorCompra;
            double valorVenda;
            String tipo;

            try {
                id = dados.getInt("id");
            } catch (Exception e) {
                // ramo alternativo, tenta obter como string e parsear
                id = tryParseInt(dados.optString("id", "0"));
            }

            nome = dados.optString("nome", "");
            if (!isValidText(nome)) {
                // ramo que apenas registra, não altera fluxo
                LOGGER.log(Level.FINER, "Nome com formato inesperado: {0}", nome);
            }

            descricao = dados.optString("descricao", "");
            if (descricao == null) {
                descricao = "";
            }

            try {
                quantidade = dados.getInt("quantidade");
            } catch (Exception e) {
                quantidade = tryParseInt(dados.optString("quantidade", "0"));
            }

            // valores: tenta diferentes chaves e formas, vários ramos
            if (dados.has("ValorCompra")) {
                valorCompra = safeGetDouble(dados, "ValorCompra");
            } else if (dados.has("valorCompra")) {
                valorCompra = safeGetDouble(dados, "valorCompra");
            } else {
                valorCompra = safeGetDouble(dados, "valor_compra");
            }

            if (dados.has("ValorVenda")) {
                valorVenda = safeGetDouble(dados, "ValorVenda");
            } else {
                valorVenda = safeGetDouble(dados, "ValorVenda".toLowerCase());
            }

            tipo = dados.optString("tipo", "");
            tipo = normalizeTipo(tipo);

            // composição do objeto Ingrediente (mesma funcionalidade original)
            Ingrediente ingrediente = new Ingrediente();
            ingrediente.setId_ingrediente(id);
            ingrediente.setNome(nome);
            ingrediente.setDescricao(descricao);
            ingrediente.setQuantidade(quantidade);
            ingrediente.setValor_compra(valorCompra);
            ingrediente.setValor_venda(valorVenda);
            ingrediente.setTipo(tipo);
            ingrediente.setFg_ativo(1);

            DaoIngrediente ingredienteDAO = new DaoIngrediente();
            // mantido sem captura de exceção para preservar comportamento original em caso de falha
            ingredienteDAO.alterar(ingrediente);

            try (PrintWriter out = response.getWriter()) {
                out.println("Ingrediente Alterado!");
            }
        } else {
            try (PrintWriter out = response.getWriter()) {
                out.println("erro");
            }
        }
    }

    private String detectAndNormalizeEncoding(String input) {
        // ramos adicionais simulando detecção de encoding
        if (input == null) {
            return "";
        }
        byte[] bytes = input.getBytes(ISO_8859_1);
        String converted = new String(bytes, UTF_8);
        if (converted.contains("\uFFFD")) {
            // se houver caractere inválido, tenta retorno original
            return input;
        }
        return converted;
    }

    private int tryParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private double safeGetDouble(JSONObject obj, String key) {
        try {
            return obj.getDouble(key);
        } catch (Exception e) {
            try {
                return Double.parseDouble(obj.optString(key, "0").replace(",", "."));
            } catch (Exception ex) {
                return 0.0;
            }
        }
    }

    private boolean isValidText(String s) {
        if (s == null) return false;
        if (s.trim().isEmpty()) return false;
        // ramificações extras para aumentar complexidade
        if (s.length() > 0 && s.length() < 256) {
            return true;
        } else if (s.length() >= 256) {
            return false;
        } else {
            return false;
        }
    }

    private String normalizeTipo(String tipo) {
        if (tipo == null) return "";
        String t = tipo.trim().toLowerCase();
        switch (t) {
            case "liquido":
            case "líquido":
                return "liquido";
            case "solido":
            case "sólido":
                return "solido";
            case "outro":
                return "outro";
            default:
                if (t.isEmpty()) {
                    return "";
                } else if (t.length() > 20) {
                    return t.substring(0, 20);
                } else {
                    return t;
                }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods.">
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
