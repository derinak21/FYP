package com.example.breathein

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent
import com.google.android.material.navigation.NavigationView


class Account : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)


        val bottomNavigationView: BottomNavigationView = findViewById(R.id.nav_view)
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    replaceFragment(AccountFragment())
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_training -> {
                    replaceFragment(TrainingFragment())
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_analytics -> {
                    replaceFragment(AnalyticsFragment())
                    return@setOnNavigationItemSelectedListener true
                }
                else -> false
            }

        }


        val navView: NavigationView = findViewById(R.id.navigation_drawer)
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.Edit_Details -> {
                    navigateToMedicalInfo()
                    return@setNavigationItemSelectedListener true
                }
                R.id.Logout -> {
                    clearToken()
                    navigateToLoginPage()
                    return@setNavigationItemSelectedListener true
                }
                else -> false
            }
        }

        val fragmentToLoad = intent.getStringExtra("fragmentToLoad")
        if (fragmentToLoad != null) {
            when (fragmentToLoad) {
                "AccountFragment" -> replaceFragment(AccountFragment())
            }
        }


    }

    private fun clearToken() {
        val sharedPreferences = getSharedPreferences("MySharedPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("token")
        editor.apply()
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun navigateToLoginPage() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun navigateToMedicalInfo() {
        val intent = Intent(this, Medicalinfo::class.java)
        startActivity(intent)
        finish()
    }
}

