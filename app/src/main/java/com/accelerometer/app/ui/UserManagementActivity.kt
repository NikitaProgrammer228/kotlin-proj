package com.accelerometer.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.accelerometer.app.R
import com.accelerometer.app.database.AppDatabase
import com.accelerometer.app.database.User
import com.accelerometer.app.databinding.ActivityUserManagementBinding
import com.accelerometer.app.databinding.DialogAddUserBinding
import com.accelerometer.app.databinding.ItemUserBinding
import kotlinx.coroutines.launch

class UserManagementActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityUserManagementBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: UserAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        database = AppDatabase.getDatabase(this)
        
        adapter = UserAdapter { user ->
            deleteUser(user)
        }
        
        binding.recyclerViewUsers.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewUsers.adapter = adapter
        
        binding.btnAddUser.setOnClickListener {
            showAddUserDialog()
        }
        
        loadUsers()
    }
    
    private fun loadUsers() {
        lifecycleScope.launch {
            database.userDao().getAllUsers().collect { users ->
                adapter.submitList(users)
            }
        }
    }
    
    private fun showAddUserDialog() {
        val dialogBinding = DialogAddUserBinding.inflate(layoutInflater)
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Добавить пользователя")
            .setView(dialogBinding.root)
            .setPositiveButton("Добавить") { _, _ ->
                val name = dialogBinding.etUserName.text.toString().trim()
                val email = dialogBinding.etUserEmail.text.toString().trim()
                
                if (name.isNotEmpty()) {
                    addUser(name, if (email.isEmpty()) null else email)
                } else {
                    Toast.makeText(this, "Введите имя пользователя", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .create()
        
        dialog.show()
    }
    
    private fun addUser(name: String, email: String?) {
        lifecycleScope.launch {
            val user = User(name = name, email = email)
            database.userDao().insertUser(user)
            Toast.makeText(this@UserManagementActivity, "Пользователь добавлен", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteUser(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Удалить пользователя?")
            .setMessage("Вы уверены, что хотите удалить ${user.name}?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    database.userDao().deleteUser(user)
                    Toast.makeText(this@UserManagementActivity, "Пользователь удален", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

class UserAdapter(
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
    
    private var users = listOf<User>()
    
    fun submitList(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }
    
    override fun getItemCount() = users.size
    
    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.tvUserName.text = user.name
            binding.tvUserEmail.text = user.email ?: ""
            binding.btnDelete.setOnClickListener {
                onDeleteClick(user)
            }
        }
    }
}

