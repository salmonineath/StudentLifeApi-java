package com.studentlife.studentlifejava.controller;

import com.studentlife.studentlifejava.dto.response.ApiResponse;
import com.studentlife.studentlifejava.dto.response.MemberResponse;
import com.studentlife.studentlifejava.service.UserSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User search endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserSearchController {

    private final UserSearchService userSearchService;

    @GetMapping("/search")
    @Operation(summary = "Search users by email prefix, for invite autocomplete (max 10 results)")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> search(@RequestParam String email) {
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Users fetched.",
                userSearchService.searchByEmail(email)));
    }
}
