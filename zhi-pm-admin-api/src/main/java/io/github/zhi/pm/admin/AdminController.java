package io.github.zhi.pm.admin;

import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller providing admin API endpoints for the gateway.
 */
@RestController
@RequestMapping("${realtime.admin.path:/admin/api}")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * List all active connections.
     */
    @GetMapping("/connections")
    public Flux<Map<String, Object>> listConnections() {
        return adminService.listConnections();
    }

    /**
     * Get connection details by session ID.
     */
    @GetMapping("/connections/{sessionId}")
    public Mono<Map<String, Object>> getConnection(@PathVariable("sessionId") String sessionId) {
        return adminService.getConnection(sessionId);
    }

    /**
     * Kick a connection by session ID.
     */
    @DeleteMapping("/connections/{sessionId}")
    public Mono<Map<String, Object>> kickConnection(@PathVariable("sessionId") String sessionId) {
        return adminService.kickConnection(sessionId)
                .map(success -> Map.of("kicked", success, "sessionId", sessionId));
    }

    /**
     * List all active rooms.
     */
    @GetMapping("/rooms")
    public Flux<Map<String, Object>> listRooms() {
        return adminService.listRooms();
    }

    /**
     * Get members of a specific room.
     */
    @GetMapping("/rooms/{roomId}/members")
    public Flux<Map<String, Object>> getRoomMembers(@PathVariable("roomId") String roomId) {
        return adminService.getRoomMembers(roomId);
    }

    /**
     * Broadcast a message to all connected clients.
     */
    @PostMapping("/broadcast")
    public Mono<Map<String, Object>> broadcast(@RequestBody Map<String, Object> body) {
        String type = (String) body.getOrDefault("type", "admin.broadcast");
        Object payload = body.get("payload");
        return adminService.broadcastMessage(type, payload);
    }

    /**
     * Get overall gateway statistics.
     */
    @GetMapping("/stats")
    public Mono<Map<String, Object>> getStats() {
        return adminService.getStats();
    }
}
