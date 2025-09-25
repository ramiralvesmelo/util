// 
package br.com.springboot.appdemo.message.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ramiralvesmelo.util.shared.dto.AuditLogDto;

class AuditLogEventTest {

	@Test
	@DisplayName("Builder deve popular campos corretamente")
	void shouldBuildWithFields() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("user", "alice");
		payload.put("action", "LOGIN");

		AuditLogDto event = AuditLogDto.builder().id("123").payload(payload).build();

		assertThat(event.getId()).isEqualTo("123");
		assertThat(event.getPayload()).containsEntry("user", "alice").containsEntry("action", "LOGIN");
	}

	@Test
	@DisplayName("Getters/Setters e construtor vazio funcionam")
	void shouldUseGettersSettersAndNoArgsConstructor() {
		AuditLogDto event = new AuditLogDto(); // @NoArgsConstructor
		assertThat(event.getId()).isNull();
		assertThat(event.getPayload()).isNull();

		Map<String, Object> payload = new HashMap<>();
		payload.put("k", 1);

		event.setId("abc");
		event.setPayload(payload);

		assertThat(event.getId()).isEqualTo("abc");
		assertThat(event.getPayload()).containsEntry("k", 1);
	}

	@Test
	@DisplayName("Construtor com todos os argumentos deve atribuir valores")
	void shouldUseAllArgsConstructor() {
		Map<String, Object> payload = Map.of("ok", true);
		AuditLogDto event = new AuditLogDto("id-1", payload);

		assertThat(event.getId()).isEqualTo("id-1");
		assertThat(event.getPayload()).isEqualTo(payload);
	}

	@Test
	@DisplayName("equals/hashCode gerados por Lombok devem funcionar")
	void equalsAndHashCode() {
		Map<String, Object> p1 = Map.of("x", 1);
		Map<String, Object> p2 = Map.of("x", 1);

		AuditLogDto a = AuditLogDto.builder().id("X").payload(p1).build();
		AuditLogDto b = AuditLogDto.builder().id("X").payload(p2).build();
		AuditLogDto c = AuditLogDto.builder().id("Y").payload(p1).build();

		assertThat(a)
			.isEqualTo(b)
			.hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo((Object) null)
			.isNotEqualTo(new Object());
	}

	@Test
	@DisplayName("toString deve conter campos principais")
	void toStringContainsFields() {
		AuditLogDto event = AuditLogDto.builder().id("T-1").payload(Map.of("key", "value")).build();

		String s = event.toString(); // @Data gera toString
		assertThat(s).contains("T-1").contains("key").contains("value");
	}

	@Test
	@DisplayName("Jackson: serializar e desserializar mantendo valores")
	void jacksonSerializationRoundTrip() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		AuditLogDto original = AuditLogDto.builder().id("J-1").payload(Map.of("a", 10, "b", "str")).build();

		String json = mapper.writeValueAsString(original);
		AuditLogDto restored = mapper.readValue(json, AuditLogDto.class);

		assertThat(restored).isEqualTo(original);
		assertThat(restored.getPayload()).containsEntry("a", 10).containsEntry("b", "str");
	}

	@Test
	@DisplayName("Objeto mutável: alterações no payload são refletidas")
	void payloadIsMutableMapReference() {
		Map<String, Object> payload = new HashMap<>();
		AuditLogDto event = AuditLogDto.builder().id("M").payload(payload).build();

		payload.put("n", 42);
		assertThat(event.getPayload()).containsEntry("n", 42);

		event.getPayload().put("m", 7);
		assertThat(payload).containsEntry("m", 7);
	}
}
