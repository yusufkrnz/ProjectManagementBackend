import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.yusufkurnaz.ProjectManagementBackend.Login.PreLogin.Authentication.Service.AuthService;
import com.yusufkurnaz.ProjectManagementBackend.Login.PreLogin.Authentication.Controller.request.LoginRequest;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.User;



@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth Controller", description = "Auth controller")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

@PostMapping("/login")
public ResponseEntity<String> login(@RequestBody LoginRequest request) {
    String token = authService.login(request);
    return ResponseEntity.ok(authService.login(request));
}

@PostMapping("/refresh-token")
public ResponseEntity<String> refreshToken(@RequestBody String token) {
    return ResponseEntity.ok(authService.refreshToken(token));
}

@PostMapping("/logout")
public ResponseEntity<String> logout(@RequestBody String token) {
    return ResponseEntity.ok(authService.logout(token));
}
@GetMapping("/me")
public ResponseEntity<User> me() {
    return ResponseEntity.ok(authService.me());
}


}