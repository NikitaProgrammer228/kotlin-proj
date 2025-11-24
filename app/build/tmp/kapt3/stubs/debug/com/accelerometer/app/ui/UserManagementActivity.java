package com.accelerometer.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.accelerometer.app.R;
import com.accelerometer.app.database.AppDatabase;
import com.accelerometer.app.database.User;
import com.accelerometer.app.databinding.ActivityUserManagementBinding;
import com.accelerometer.app.databinding.DialogAddUserBinding;
import com.accelerometer.app.databinding.ItemUserBinding;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u001a\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\fH\u0002J\u0010\u0010\u000e\u001a\u00020\n2\u0006\u0010\u000f\u001a\u00020\u0010H\u0002J\b\u0010\u0011\u001a\u00020\nH\u0002J\u0012\u0010\u0012\u001a\u00020\n2\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014H\u0014J\b\u0010\u0015\u001a\u00020\u0016H\u0016J\b\u0010\u0017\u001a\u00020\nH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"Lcom/accelerometer/app/ui/UserManagementActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "adapter", "Lcom/accelerometer/app/ui/UserAdapter;", "binding", "Lcom/accelerometer/app/databinding/ActivityUserManagementBinding;", "database", "Lcom/accelerometer/app/database/AppDatabase;", "addUser", "", "name", "", "email", "deleteUser", "user", "Lcom/accelerometer/app/database/User;", "loadUsers", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onSupportNavigateUp", "", "showAddUserDialog", "app_debug"})
public final class UserManagementActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.accelerometer.app.databinding.ActivityUserManagementBinding binding;
    private com.accelerometer.app.database.AppDatabase database;
    private com.accelerometer.app.ui.UserAdapter adapter;
    
    public UserManagementActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void loadUsers() {
    }
    
    private final void showAddUserDialog() {
    }
    
    private final void addUser(java.lang.String name, java.lang.String email) {
    }
    
    private final void deleteUser(com.accelerometer.app.database.User user) {
    }
    
    @java.lang.Override()
    public boolean onSupportNavigateUp() {
        return false;
    }
}