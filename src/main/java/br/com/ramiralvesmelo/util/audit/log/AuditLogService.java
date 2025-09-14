package br.com.ramiralvesmelo.util.audit.log;

import br.com.ramiralvesmelo.util.message.event.AuditLogEvent;

public interface AuditLogService{

	public void send(AuditLogEvent event);
	
}
