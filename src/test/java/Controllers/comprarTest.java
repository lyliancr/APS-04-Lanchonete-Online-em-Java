package Controllers;

import DAO.*;
import Helpers.ValidadorCookie;
import Model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class comprarTest {

	//setup
	private static final String JSON_VALIDO = "{ \"id\": 1, \"Xsalada\": [\"10.0\", \"lanche\", 2], \"Coca\": [\"5.0\", \"bebida\", 1] }";
	private HttpServletRequest request;
	private HttpServletResponse response;
	private PrintWriter printWriter;
	private StringWriter stringWriter;

	private BufferedReader createMockBufferedReader(String content) throws IOException {
		BufferedReader mockReader = mock(BufferedReader.class);

		// se o conteúdo for vazio retorna uma string vazia
		if (content.isEmpty()) {
			when(mockReader.readLine()).thenReturn("").thenReturn(null);
		} else {
			when(mockReader.readLine()).thenReturn(content).thenReturn(null);
		}
		return mockReader;
	}

	// simula getInputStream()
	static class TestServletInputStream extends ServletInputStream {
		private final InputStream stream;

		public TestServletInputStream(byte[] data) {
			this.stream = new ByteArrayInputStream(data);
		}

		@Override
		public int read() throws IOException {
			return stream.read();
		}

		@Override
		public boolean isFinished() {
			return false;
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setReadListener(javax.servlet.ReadListener listener) {
		}
	}

	@BeforeEach
	void setup() throws IOException {
		request = mock(HttpServletRequest.class);
		response = mock(HttpServletResponse.class);
		stringWriter = new StringWriter();
		printWriter = new PrintWriter(stringWriter);
		when(response.getWriter()).thenReturn(printWriter);
	}

	// TESTE 1: FLUXO DE SUCESSO COMPLETO
	@Test
	void deveSalvarPedidoEVincularItensCorretamente() throws Exception {

		
		when(request.getInputStream())
				.thenReturn(new TestServletInputStream(JSON_VALIDO.getBytes(StandardCharsets.UTF_8)));
		when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("auth", "ok") });

		try (MockedConstruction<ValidadorCookie> mockedValidador = mockConstruction(ValidadorCookie.class,
				(mock, ctx) -> when(mock.validar(any())).thenReturn(true));
				MockedConstruction<DaoCliente> mockedCliente = mockConstruction(DaoCliente.class,
						(mock, ctx) -> when(mock.pesquisaPorID("1")).thenReturn(new Cliente()));
				MockedConstruction<DaoLanche> mockedLanche = mockConstruction(DaoLanche.class, (mock, ctx) -> {
					Lanche l = new Lanche();
					l.setValor_venda(10.0);
					when(mock.pesquisaPorNome("Xsalada")).thenReturn(l);
				});
				MockedConstruction<DaoBebida> mockedBebida = mockConstruction(DaoBebida.class, (mock, ctx) -> {
					Bebida b = new Bebida();
					b.setValor_venda(5.0);
					when(mock.pesquisaPorNome("Coca")).thenReturn(b);
				});
				MockedConstruction<DaoPedido> mockedPedido = mockConstruction(DaoPedido.class,
						(mock, ctx) -> when(mock.pesquisaPorData(any())).thenReturn(new Pedido()))) {
			
			new comprar().doPost(request, response);
			printWriter.flush();

			
			DaoPedido pedidoMock = mockedPedido.constructed().get(0);

			// Verifica o cálculo e salvamento (valor total: 2*10.0 + 1*5.0 = 25.0)
			verify(pedidoMock, times(1)).salvar(Mockito.argThat(pedido -> {
				assertEquals(25.0, pedido.getValor_total(), 0.001);
				return true;
			}));

			// Verifica as vinculações
			verify(pedidoMock, times(1)).vincularLanche(any(), any());
			verify(pedidoMock, times(1)).vincularBebida(any(), any());
			assertTrue(stringWriter.toString().contains("Pedido Salvo com Sucesso!"));
		}
	}

	// TESTE 2: FALHA NA AUTENTICAÇÃO (COOKIE)
	@Test
	void deveRetornarErroSeCookieForInvalido() throws Exception {

		
		when(request.getInputStream())
				.thenReturn(new TestServletInputStream(JSON_VALIDO.getBytes(StandardCharsets.UTF_8)));
		when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("auth", "fail") });

		try (
				// Mock SÓ o Validador e garante que ele retorne FALSE
				MockedConstruction<ValidadorCookie> mockedValidador = mockConstruction(ValidadorCookie.class,
						(mock, ctx) -> when(mock.validar(any())).thenReturn(false))) {
			
			new comprar().doPost(request, response);
			printWriter.flush();

			
			assertTrue(stringWriter.toString().contains("erro"));

			
			try (MockedConstruction<DaoCliente> mockedCliente = mockConstruction(DaoCliente.class)) {
				// Executa novamente DENTRO do mock para checar a construção
				new comprar().doPost(request, response);
				assertEquals(0, mockedCliente.constructed().size(),
						"Nenhum DAO deveria ser instanciado após falha no cookie.");
			}
		}
	}

	// TESTE 3: FALHA NO CORPO DA REQUISIÇÃO (Teste o caminho de 'json == null')
	@Test
	void deveRetornarErroSeCorpoDaRequisicaoEstiverVazio() throws Exception {

		
		when(request.getInputStream()).thenReturn(new TestServletInputStream(new byte[0]));

		
		when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("auth", "ok") });

		try (
				MockedConstruction<ValidadorCookie> mockedValidador = mockConstruction(ValidadorCookie.class,
						(mock, ctx) -> when(mock.validar(any())).thenReturn(true))) {
			
			new comprar().doPost(request, response);
			printWriter.flush();

			assertTrue(stringWriter.toString().contains("erro"),
					"O Servlet deve retornar 'erro' quando o corpo da requisição é vazio.");

			
			try (MockedConstruction<DaoCliente> mockedCliente = mockConstruction(DaoCliente.class)) {
				
				new comprar().doPost(request, response);
				assertEquals(0, mockedCliente.constructed().size(), "Nenhum DAOCliente deveria ser instanciado.");
			}
		}

	}
}