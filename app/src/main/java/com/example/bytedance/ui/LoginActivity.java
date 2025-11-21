package com.example.bytedance.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bytedance.R;
import com.example.bytedance.auth.UserDatabaseHelper;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TextInputLayout tilEmail = findViewById(R.id.tilEmail);
        TextInputLayout tilPassword = findViewById(R.id.tilPassword);
        TextInputEditText emailEt = findViewById(R.id.etEmail);
        TextInputEditText pwdEt = findViewById(R.id.etPassword);
        Button loginBtn = findViewById(R.id.btnLogin);
        TextView tvForget = findViewById(R.id.tvForget);
        Button btnWeChat = findViewById(R.id.btnWeChat);
        Button btnApple = findViewById(R.id.btnApple);

        UserDatabaseHelper db = new UserDatabaseHelper(this);

        tvForget.setOnClickListener(v -> Toast.makeText(this, "忘记密码（示例）", Toast.LENGTH_SHORT).show());
        btnWeChat.setOnClickListener(v -> Toast.makeText(this, "微信登录（示例）", Toast.LENGTH_SHORT).show());
        btnApple.setOnClickListener(v -> Toast.makeText(this, "Apple 登录（示例）", Toast.LENGTH_SHORT).show());

        loginBtn.setOnClickListener(v -> {
            String email = emailEt.getText() != null ? emailEt.getText().toString().trim() : "";
            String pwd = pwdEt.getText() != null ? pwdEt.getText().toString().trim() : "";

            // 清理旧错误
            tilEmail.setError(null);
            tilPassword.setError(null);

            boolean hasError = false;
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError("邮箱格式不正确");
                hasError = true;
            }
            if (pwd.length() < 6) {
                tilPassword.setError("密码至少6位");
                hasError = true;
            }
            if (hasError) return;
            boolean ok = db.validateUser(email, pwd);
            if (ok) {
                String name = db.getNameByEmail(email);
                android.content.SharedPreferences sp = getSharedPreferences("user_prefs", MODE_PRIVATE);
                android.content.SharedPreferences.Editor e = sp.edit();
                e.putBoolean("logged_in", true);
                e.putString("username", name != null ? name : email);
                if (!sp.contains("signature")) {
                    e.putString("signature", "欢迎来到信息App");
                }
                e.apply();
                Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
            } else {
                Toast.makeText(this, "账号或密码错误", Toast.LENGTH_SHORT).show();
            }
        });
    }
}