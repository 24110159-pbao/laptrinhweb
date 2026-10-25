package example.com.ltw.ai.controller;

import example.com.ltw.ai.dto.AiChatRequest;
import example.com.ltw.ai.dto.AiChatResponse;
import example.com.ltw.ai.service.AdminAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/ai-chatbox")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiController {

    private final AdminAiService adminAiService;

    @PostMapping("/chat")
    public AiChatResponse chat(
            @Valid @RequestBody AiChatRequest request
    ) {

        return adminAiService.chat(
                request.question().trim()
        );
    }

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<AiChatResponse>
    handleInvalidRequest(
            MethodArgumentNotValidException exception
    ) {

        String message =
                exception
                        .getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .findFirst()
                        .map(FieldError::getDefaultMessage)
                        .orElse(
                                "Yêu cầu không hợp lệ."
                        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new AiChatResponse(
                                message,
                                false
                        )
                );
    }
}
