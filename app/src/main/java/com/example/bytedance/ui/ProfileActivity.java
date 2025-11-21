package com.example.bytedance.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bytedance.R;

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextView tvName = findViewById(R.id.tvName);
        TextView tvSign = findViewById(R.id.tvSignature);
        ImageView avatar = findViewById(R.id.ivAvatar);
        android.widget.Button btnLogout = findViewById(R.id.btnLogout);

        android.content.SharedPreferences sp = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String name = sp.getString("username", "用户名");
        String sign = sp.getString("signature", "欢迎来到信息App");
        tvName.setText(name);
        tvSign.setText(sign);

        // 加载已保存头像
        String avatarPath = sp.getString("avatar_path", null);
        if (avatarPath != null) {
            try {
                Bitmap bm = BitmapFactory.decodeFile(avatarPath);
                if (bm != null) avatar.setImageBitmap(bm);
            } catch (Exception ignored) {}
        }

        // 选择并裁剪头像（中心裁剪为正方形，缩放为256x256）
        ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                (Uri uri) -> {
                    if (uri == null) return;
                    try {
                        java.io.InputStream is = getContentResolver().openInputStream(uri);
                        if (is == null) return;
                        Bitmap src = BitmapFactory.decodeStream(is);
                        is.close();
                        if (src == null) return;
                        Bitmap square = cropCenterSquare(src);
                        Bitmap scaled = Bitmap.createScaledBitmap(square, 256, 256, true);
                        String saved = saveBitmapToFile(scaled);
                        if (saved != null) {
                            avatar.setImageBitmap(scaled);
                            sp.edit().putString("avatar_path", saved).apply();
                            Toast.makeText(this, "头像已更新", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "头像更新失败", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        avatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // 退出登录：仅清理登录态标记
        btnLogout.setOnClickListener(v -> {
            sp.edit().putBoolean("logged_in", false).apply();
            Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
            startActivity(new android.content.Intent(this, com.example.bytedance.ui.LoginActivity.class));
            finish();
        });

        int[] rowIds = new int[]{R.id.rowProfile, R.id.rowFavorites, R.id.rowHistory, R.id.rowSettings, R.id.rowAbout, R.id.rowFeedback};
        String[] labels = new String[]{"个人信息", "我的收藏", "浏览历史", "设置", "关于我们", "意见反馈"};
        for (int i = 0; i < rowIds.length; i++) {
            LinearLayout row = findViewById(rowIds[i]);
            final String label = labels[i];
            row.setOnClickListener(v -> Toast.makeText(this, "点击了" + label, Toast.LENGTH_SHORT).show());
        }
    }

    private Bitmap cropCenterSquare(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int size = Math.min(w, h);
        int x = (w - size) / 2;
        int y = (h - size) / 2;
        try {
            return Bitmap.createBitmap(src, x, y, size, size);
        } catch (Exception e) {
            return src;
        }
    }

    private String saveBitmapToFile(Bitmap bm) {
        try {
            java.io.File dir = new java.io.File(getFilesDir(), "avatars");
            if (!dir.exists()) dir.mkdirs();
            java.io.File f = new java.io.File(dir, "avatar_" + System.currentTimeMillis() + ".png");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            return f.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
}