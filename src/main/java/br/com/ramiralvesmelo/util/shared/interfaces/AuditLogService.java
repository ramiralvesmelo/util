package br.com.ramiralvesmelo.util.shared.interfaces;

import br.com.ramiralvesmelo.util.shared.event.AuditLogEvent;

public interface AuditLogService{

	public void send(AuditLogEvent event);
	
}
