package no.kommune.homecare.websocket;

import lombok.RequiredArgsConstructor;
import no.kommune.homecare.websocket.dto.HandoverReportMessage;
import no.kommune.homecare.websocket.dto.RedistributionMessage;
import no.kommune.homecare.websocket.dto.UrgentAlertMessage;
import no.kommune.homecare.websocket.dto.VisitUpdateMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Single place from which domain services broadcast real-time updates to
 * connected clients over STOMP/WebSocket. Keeps services free of transport
 * concerns (destinations, message shape).
 */
@Component
@RequiredArgsConstructor
public class WebSocketEventPublisher {

    private static final String VISITS_TOPIC = "/topic/visits";
    private static final String URGENT_ALERTS_TOPIC = "/topic/alerts/urgent";
    private static final String REDISTRIBUTION_TOPIC = "/topic/absences/redistribution";
    private static final String HANDOVER_TOPIC = "/topic/handover-reports";

    private final SimpMessagingTemplate messagingTemplate;

    public void publishVisitUpdate(VisitUpdateMessage message) {
        messagingTemplate.convertAndSend(VISITS_TOPIC, message);
        messagingTemplate.convertAndSend("/topic/nurses/" + message.nurseId() + "/visits", message);
    }

    public void publishUrgentAlert(UrgentAlertMessage message) {
        messagingTemplate.convertAndSend(URGENT_ALERTS_TOPIC, message);
    }

    public void publishRedistribution(RedistributionMessage message) {
        messagingTemplate.convertAndSend(REDISTRIBUTION_TOPIC, message);
    }

    public void publishHandoverReport(HandoverReportMessage message) {
        messagingTemplate.convertAndSend(HANDOVER_TOPIC, message);
    }
}
