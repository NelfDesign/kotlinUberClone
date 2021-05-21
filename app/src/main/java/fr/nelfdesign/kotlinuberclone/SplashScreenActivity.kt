package fr.nelfdesign.kotlinuberclone

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import fr.nelfdesign.kotlinuberclone.model.DriverInfoModel
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_splash_screen.*
import java.util.*
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    /*companion object{
        private val LOGIN_REQUEST_CODE = 7171
    }*/

    private lateinit var providers:List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth:FirebaseAuth
    private lateinit var listener:FirebaseAuth.AuthStateListener

    private lateinit var database: FirebaseDatabase
    private lateinit var driverInfoRef: DatabaseReference

    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    override fun onStop() {
        if (firebaseAuth != null && listener != null) firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }

    @SuppressLint("CheckResult")
    private fun delaySplashScreen() {
        Completable.timer(3,TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe {
                    firebaseAuth.addAuthStateListener(listener)
                }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        init()
    }

    private fun init(){
        database = FirebaseDatabase.getInstance()
        driverInfoRef = database.getReference(Common.DRIVER_INFO_REFERENCE)

        providers = listOf(
                AuthUI.IdpConfig.PhoneBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build()
        )
        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener { myFirebaseAuth ->
            val user = myFirebaseAuth.currentUser

            if (user != null){
                Log.w("splash", "user ok")
                checkUserFromFirebase()
            } else{
                showLoginLayout()
            }
        }
    }

    private fun checkUserFromFirebase() {
        Log.d("splash", "user= " + FirebaseAuth.getInstance().currentUser!!.uid)
        driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                .addListenerForSingleValueEvent(object:ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.w("splash", "ok layout")
                        if (snapshot.exists()){
                            Toast.makeText(this@SplashScreenActivity, "User already register!", Toast.LENGTH_SHORT).show()
                        }else{
                            Log.w("splash", "ok layout")
                            showRegisterLayout()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                      Toast.makeText(this@SplashScreenActivity, error.message, Toast.LENGTH_SHORT).show()
                    }

                })
    }

    private fun showRegisterLayout() {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val itemview = LayoutInflater.from(this).inflate(R.layout.layout_register, null)

        val edt_first_name = itemview.findViewById<View>(R.id.edit_first_name) as TextInputEditText
        val edt_last_name = itemview.findViewById<View>(R.id.edit_last_name) as TextInputEditText
        val edt_phone_number = itemview.findViewById<View>(R.id.edit_phone_number) as TextInputEditText
        val btn_continue = itemview.findViewById<View>(R.id.btn_register) as Button

        //set data
        if (FirebaseAuth.getInstance().currentUser!!.phoneNumber != null
            && !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber)){
            edt_phone_number.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)
        }
        //view
        builder.setView(itemview)
        val dialog = builder.create()
        dialog.show()
        //event
        btn_continue.setOnClickListener {
            if (TextUtils.isDigitsOnly(edt_first_name.text.toString())){
                Toast.makeText(this@SplashScreenActivity, "Please enter first name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else if (TextUtils.isDigitsOnly(edt_last_name.text.toString())){
                Toast.makeText(this@SplashScreenActivity, "Please enter last name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else if (TextUtils.isDigitsOnly(edt_phone_number.text.toString())){
                Toast.makeText(this@SplashScreenActivity, "Please enter phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else{
                val driver = DriverInfoModel()
                driver.firstName = edt_first_name.text.toString()
                driver.lastName = edt_last_name.text.toString()
                driver.phoneNumber = edt_phone_number.text.toString()
                driver.rating = 0.0
                //add to database
                driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(driver)
                    .addOnFailureListener{
                            e -> Toast.makeText(this@SplashScreenActivity, e.message, Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                           progress_bar.visibility = View.GONE
                    }
                    .addOnSuccessListener {
                        Toast.makeText(this@SplashScreenActivity, "Register Successfully", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        progress_bar.visibility = View.GONE
                    }
            }
        }
    }

    private fun showLoginLayout() {
        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
                .setPhoneButtonId(R.id.btn_phone_sign_in)
                .setGoogleButtonId(R.id.btn_google_sign_in)
                .build()

        startForResult.launch(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build())
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        val response = IdpResponse.fromResultIntent(result.data)
        if (result.resultCode == Activity.RESULT_OK) {
            val user = FirebaseAuth.getInstance().currentUser
        }else{
            Toast.makeText(this@SplashScreenActivity,response!!.error!!.message, Toast.LENGTH_SHORT).show()
        }
    }

}