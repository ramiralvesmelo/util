package br.com.ramiralvesmelo.util.shared.interfaces;

import br.com.ramiralvesmelo.util.shared.dto.AuditLogDto;

public interface AuditLogService{

	public void send(AuditLogDto event);
	
}
