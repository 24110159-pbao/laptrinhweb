package example.com.ltw.shipping.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.com.ltw.shipping.service.ShippingWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ShippingWebhookController {

    private final ShippingWebhookService webhookService;

    @PostMapping(value = "/shipping/ghn/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> webhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-GHN-Signature", required = false) String signature
    ) {
        if (!webhookService.verifySignature(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid signature"));
        }
        try {
            JsonNode event = new ObjectMapper().readTree(rawBody);
            if (event == null || !event.isObject()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid JSON payload"));
            }
            webhookService.handle(
                    text(event, "eventId"),
                    text(event, "orderCode"),
                    text(event, "status")
            );
            return ResponseEntity.ok(Map.of("message", "Accepted"));
        } catch (IOException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid JSON payload"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    private String text(JsonNode node, String name) {
        JsonNode value = node.get(name);
        return value != null && value.isTextual() ? value.asText() : null;
    }
}
