package com.june.insta

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.june.insta.navigation.AlarmFragment
import com.june.insta.navigation.DetailViewFragment
import com.june.insta.navigation.GridFragment
import com.june.insta.navigation.UserFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottom_navigation.setOnNavigationItemSelectedListener(this)
    }//onCreate

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_home -> {
                var detailViewFragment = DetailViewFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, detailViewFragment).commit()
                return true
            }//home

            R.id.action_search -> {
                var gridFragment = GridFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, gridFragment).commit()
                return true
            }//search

            R.id.action_add_photo -> {
                return true
            }//photo

            R.id.action_favorite_alarm -> {
                var alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, alarmFragment).commit()
                return true
            }//alarm

            R.id.action_account -> {
                var userFragment = UserFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, userFragment).commit()
                return true
            }//account
        }//when
        return false
    }//fun
}