package example.com.ltw.chat.controller;

import example.com.ltw.auth.service.AuthService;
import example.com.ltw.chat.dto.ChatMessageDto;
import example.com.ltw.chat.dto.SendMessageRequest;
import example.com.ltw.chat.entity.ChatConversation;
import example.com.ltw.chat.service.ChatService;
import example.com.ltw.user.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    private User currentUser(HttpSession session) {
        Object val = session.getAttribute(AuthService.SESSION_CURRENT_USER);
        return val instanceof User u ? u : null;
    }

    /**
     * REST endpoint: Lấy cuộc trò chuyện đang hoạt động của khách hàng.
     */
    @GetMapping("/api/chat/active")
    @ResponseBody
    public ResponseEntity<?> getActiveConversation(
            @RequestParam(name = "branchId", required = false) Long branchId,
            HttpSession session
    ) {
        User user = currentUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Vui lòng đăng nhập để sử dụng tính năng Chat!", "authenticated", false));
        }

        try {
            ChatConversation conv = chatService.getOrCreateConversation(user, branchId);
            List<ChatMessageDto> messages = chatService.getConversationMessages(conv.getId(), user);

            Map<String, Object> resp = new HashMap<>();
            resp.put("authenticated", true);
            resp.put("conversationId", conv.getId());
            resp.put("branchId", conv.getBranch() != null ? conv.getBranch().getId() : null);
            resp.put("branchName", conv.getBranch() != null ? conv.getBranch().getName() : "NexusTech Store");
            resp.put("currentUserId", user.getId());
            resp.put("currentUserName", user.getFullName());
            resp.put("currentUserRole", user.getRole().name());
            resp.put("messages", messages);
            resp.put("branches", chatService.getAvailableBranchesForUser(user));
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            log.error("Lỗi khi mở cuộc trò chuyện: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * REST endpoint: Lấy danh sách các chi nhánh hỗ trợ chat.
     */
    @GetMapping("/api/chat/branches")
    @ResponseBody
    public ResponseEntity<?> getBranches(HttpSession session) {
        User user = currentUser(session);
        return ResponseEntity.ok(chatService.getAvailableBranchesForUser(user));
    }

    /**
     * REST endpoint: Lấy tin nhắn của cuộc trò chuyện.
     */
    @GetMapping("/api/chat/{conversationId}/messages")
    @ResponseBody
    public ResponseEntity<?> getMessages(
            @PathVariable Long conversationId,
            HttpSession session
    ) {
        User user = currentUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Chưa đăng nhập!"));
        }

        List<ChatMessageDto> messages = chatService.getConversationMessages(conversationId, user);
        return ResponseEntity.ok(messages);
    }

    /**
     * REST endpoint: Gửi tin nhắn (hỗ trợ cả REST lẫn WebSocket).
     */
    @PostMapping("/api/chat/{conversationId}/send")
    @ResponseBody
    public ResponseEntity<?> sendMessageRest(
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request,
            HttpSession session
    ) {
        User user = currentUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Chưa đăng nhập!"));
        }

        try {
            ChatMessageDto msg = chatService.sendMessage(conversationId, user, request.getContent());
            return ResponseEntity.ok(msg);
        } catch (Exception ex) {
            log.error("Lỗi khi gửi tin nhắn REST: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * WebSocket MessageMapping: Khách hoặc Staff gửi tin nhắn qua STOMP /app/chat.sendMessage.
     */
    @MessageMapping("/chat.sendMessage")
    public void handleWebSocketMessage(@Payload SendMessageRequest payload) {
        log.info("Nhận tin nhắn qua WebSocket STOMP cho conversation #{}: {}", payload.getConversationId(), payload.getContent());
    }
}
