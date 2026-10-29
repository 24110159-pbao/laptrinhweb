package example.com.ltw.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private Long conversationId;
    private Long branchId;
    private Long senderId;
    private String senderName;
    private String senderRole;
    private String senderAvatar;
    private String content;
    private String createdAt;

    @JsonProperty("isMe")
    private boolean isMe;

    @JsonProperty("isOrderNotice")
    private boolean isOrderNotice;

    @JsonProperty("me")
    public boolean getMe() {
        return isMe;
    }

    @JsonProperty("orderNotice")
    public boolean getOrderNotice() {
        return isOrderNotice;
    }
}
