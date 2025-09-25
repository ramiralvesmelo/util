package br.com.ramiralvesmelo.util.commons.interfaces;

import br.com.ramiralvesmelo.util.commons.dto.AuditLogDto;

public interface AuditLogStoreService{

	public void send(AuditLogDto event);
	
}
