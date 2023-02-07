package com.example.mapall

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.mapall.databinding.ActivityLoginBinding




class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.saveBtn.setOnClickListener{
            saveData()
        }
    }

    private fun saveData()
    {
        val name = binding.NameEditTx.text.toString()
        val lastName = binding.LastNameEditTx.text.toString()
        val date = binding.birthDateEditTxt.text.toString()

        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.apply{
            putString("Name",name)
            putString("LastName", lastName)
            putString("Date", date)
        }
        Toast.makeText(this, "uloženo", Toast.LENGTH_SHORT).show()
    }
}