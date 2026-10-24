package example.com.ltw.chat.service;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.branch.repository.BranchRepository;
import example.com.ltw.chat.dto.ChatMessageDto;
import example.com.ltw.chat.entity.ChatConversation;
import example.com.ltw.chat.entity.ChatConversationStatus;
import example.com.ltw.chat.entity.ChatMessage;
import example.com.ltw.chat.repository.ChatConversationRepository;
import example.com.ltw.chat.repository.ChatMessageRepository;
import example.com.ltw.order.entity.Order;
import example.com.ltw.user.entity.Role;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    /**
     * Lấy hoặc tạo cuộc trò chuyện đang mở giữa User và Chi nhánh.
     */
    @Transactional
    public ChatConversation getOrCreateConversation(User user, Long branchId) {
        if (user == null) {
            throw new IllegalArgumentException("Người dùng chưa đăng nhập!");
        }

        Branch branch = null;
        if (branchId != null && branchId > 0) {
            branch = branchRepository.findById(branchId).orElse(null);
        }
        if (branch == null && user.getBranch() != null) {
            branch = user.getBranch();
        }
        if (branch == null) {
            branch = branchRepository.findAll().stream().findFirst().orElseThrow(
                    () -> new IllegalStateException("Hệ thống chưa có chi nhánh nào để chat!")
            );
        }

        final Branch targetBranch = branch;
        return conversationRepository.findFirstByUserIdAndBranchIdAndStatus(user.getId(), targetBranch.getId(), ChatConversationStatus.OPEN)
                .orElseGet(() -> {
                    ChatConversation conv = new ChatConversation();
                    conv.setUser(user);
                    conv.setBranch(targetBranch);
                    conv.setStatus(ChatConversationStatus.OPEN);
                    return conversationRepository.save(conv);
                });
    }

    /**
     * Gửi tin nhắn trong cuộc trò chuyện và bắn realtime qua WebSocket.
     */
    @Transactional
    public ChatMessageDto sendMessage(Long conversationId, User sender, String content) {
        if (conversationId == null) {
            throw new IllegalArgumentException("Mã cuộc trò chuyện không hợp lệ!");
        }
        if (content == null || content.trim().isBlank()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống!");
        }

        ChatConversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc trò chuyện #" + conversationId));

        if (conv.getStatus() == ChatConversationStatus.CLOSED) {
            conv.setStatus(ChatConversationStatus.OPEN);
        }

        // Nếu người gửi là Nhân viên / Quản lý / Admin thì gán nhân viên phụ trách nếu chưa có
        if (sender.getRole() == Role.STAFF || sender.getRole() == Role.MANAGER || sender.getRole() == Role.ADMIN) {
            if (conv.getStaff() == null) {
                conv.setStaff(sender);
            }
        }

        ChatMessage message = new ChatMessage();
        message.setConversation(conv);
        message.setSender(sender);
        message.setContent(content.trim());
        message.setCreatedAt(LocalDateTime.now());
        ChatMessage saved = messageRepository.save(message);

        conv.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conv);

        ChatMessageDto dto = toDto(saved, sender.getId());

        // Bắn WebSocket đến topic của cuộc trò chuyện và topic của chi nhánh
        try {
            messagingTemplate.convertAndSend("/topic/chat/" + conversationId, dto);
            if (conv.getBranch() != null) {
                messagingTemplate.convertAndSend("/topic/branch/" + conv.getBranch().getId() + "/chat", dto);
            }
        } catch (Exception ex) {
            log.warn("Không thể gửi thông báo WebSocket (sẽ dùng HTTP polling dự phòng): {}", ex.getMessage());
        }

        return dto;
    }

    /**
     * Khi mua hàng thành công, Nhân viên (Staff) chi nhánh gửi tin nhắn xác nhận tiếp nhận đơn cho Khách Hàng (User).
     */
    @Transactional
    public void sendOrderSuccessMessage(Order order) {
        if (order == null || order.getUser() == null) {
            return;
        }

        try {
            User customer = order.getUser();
            Long branchId = order.getBranch() != null ? order.getBranch().getId() : null;
            ChatConversation conversation = getOrCreateConversation(customer, branchId);

            // Tìm nhân viên Staff phụ trách của chi nhánh
            User staffSender = null;
            if (conversation.getStaff() != null) {
                staffSender = conversation.getStaff();
            } else if (branchId != null) {
                List<User> branchStaffs = userRepository.findAllByBranch_IdAndRoleOrderByCreatedAtDesc(branchId, Role.STAFF);
                if (!branchStaffs.isEmpty()) {
                    staffSender = branchStaffs.get(0);
                } else {
                    List<User> branchManagers = userRepository.findAllByBranch_IdAndRoleOrderByCreatedAtDesc(branchId, Role.MANAGER);
                    if (!branchManagers.isEmpty()) {
                        staffSender = branchManagers.get(0);
                    }
                }
            }

            // Fallback nếu chi nhánh chưa có nhân viên
            if (staffSender == null) {
                List<User> anyStaff = userRepository.findAllByRole(Role.STAFF);
                if (!anyStaff.isEmpty()) {
                    staffSender = anyStaff.get(0);
                } else {
                    List<User> anyAdmin = userRepository.findAllByRole(Role.ADMIN);
                    if (!anyAdmin.isEmpty()) {
                        staffSender = anyAdmin.get(0);
                    }
                }
            }

            if (staffSender != null && conversation.getStaff() == null) {
                conversation.setStaff(staffSender);
                conversationRepository.save(conversation);
            }

            User sender = (staffSender != null) ? staffSender : customer;
            String branchName = order.getBranch() != null ? order.getBranch().getName() : "NexusTech";
            String amountFormatted = String.format("%,d ₫", order.getFinalAmount().longValue());

            String autoMessage = "🎉 [XÁC NHẬN TIẾP NHẬN ĐƠN HÀNG]\n"
                    + "Dạ chào bạn " + (order.getReceiverName() != null ? order.getReceiverName() : customer.getFullName()) + "! NexusTech (" + branchName + ") xin thông báo đã tiếp nhận đơn hàng #" + order.getOrderCode() + " của bạn:\n"
                    + "• Tổng thanh toán: " + amountFormatted + "\n"
                    + "• Hình thức: " + (order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "COD")
                    + " (" + (order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "PENDING") + ")\n"
                    + "• Người nhận: " + order.getReceiverName() + " (" + order.getReceiverPhone() + ")\n"
                    + "• Địa chỉ giao: " + order.getShippingStreetAddress() + ", " + order.getShippingWard() + ", " + order.getShippingDistrict() + ", " + order.getShippingProvince() + "\n"
                    + "Nhân viên chi nhánh đang kiểm tra và chuẩn bị đóng gói sản phẩm cho bạn nhé. Nếu bạn cần thay đổi hay hỗ trợ thêm thông tin gì, cứ nhắn trực tiếp với shop tại đây nhé ạ! ❤️";

            sendMessage(conversation.getId(), sender, autoMessage);
            log.info("Staff '{}' đã gửi tin nhắn xác nhận đơn hàng '{}' cho khách hàng '{}'",
                    sender.getEmail(), order.getOrderCode(), customer.getEmail());
        } catch (Exception ex) {
            log.error("Lỗi khi Staff gửi tin nhắn xác nhận đơn hàng thành công: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Lấy toàn bộ tin nhắn của cuộc trò chuyện (đánh dấu đã đọc).
     */
    @Transactional
    public List<ChatMessageDto> getConversationMessages(Long conversationId, User viewer) {
        if (conversationId == null) return Collections.emptyList();

        List<ChatMessage> messages = messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversationId);
        if (viewer != null) {
            messageRepository.markAllAsRead(conversationId, viewer.getId(), LocalDateTime.now());
        }

        return messages.stream()
                .map(m -> toDto(m, viewer != null ? viewer.getId() : null))
                .toList();
    }

    /**
     * Lấy danh sách cuộc trò chuyện thuộc chi nhánh cho Staff CRM.
     */
    @Transactional(readOnly = true)
    public List<ChatConversation> getBranchConversations(Long branchId) {
        if (branchId == null) return Collections.emptyList();
        return conversationRepository.findAllByBranchIdOrderByUpdatedAtDesc(branchId);
    }

    private ChatMessageDto toDto(ChatMessage message, Long currentUserId) {
        User sender = message.getSender();
        boolean isMe = (currentUserId != null && sender != null && currentUserId.equals(sender.getId()));
        String avatar = sender != null && sender.getAvatarUrl() != null && !sender.getAvatarUrl().isBlank()
                ? sender.getAvatarUrl()
                : (sender != null && sender.getRole() == Role.USER
                        ? "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=150&q=80"
                        : "https://images.unsplash.com/photo-1580489944761-15a19d654956?auto=format&fit=crop&w=150&q=80");

        boolean isNotice = message.getContent() != null && 
                (message.getContent().contains("[THÔNG BÁO ĐẶT HÀNG THÀNH CÔNG]") 
                 || message.getContent().contains("[XÁC NHẬN TIẾP NHẬN ĐƠN HÀNG]"));

        return ChatMessageDto.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(sender != null ? sender.getId() : null)
                .senderName(sender != null ? sender.getFullName() : "Người dùng")
                .senderRole(sender != null ? sender.getRole().name() : "USER")
                .senderAvatar(avatar)
                .content(message.getContent())
                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt().format(TIME_FORMATTER) : "")
                .isMe(isMe)
                .isOrderNotice(isNotice)
                .build();
    }
}
