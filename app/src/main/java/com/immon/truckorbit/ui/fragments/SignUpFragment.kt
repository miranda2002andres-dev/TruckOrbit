package com.immon.truckorbit.ui.fragments

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.immon.truckorbit.data.Constants.USER_DATABASE
import com.immon.truckorbit.data.models.UserModel
import com.immon.truckorbit.databinding.FragmentSignUpBinding
import com.immon.truckorbit.ui.activities.MainActivity.Companion.myFragmentManager
import com.immon.truckorbit.ui.fragments.base.BaseFragment


class SignUpFragment : BaseFragment() {

    private lateinit var binding: FragmentSignUpBinding
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance();

    override val isLightStatusbar: Boolean
        get() = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignUpBinding.inflate(inflater, container, false)

        binding.btnSignUp.setOnClickListener {
            binding.btnSignUp.startAnimation {
                registerUser()
            }
        }

        binding.txtSignIn.setOnClickListener {
            myFragmentManager.popBackStack()
        }

        return binding.root
    }

    private fun registerUser() {
        val name: String = binding.etName.getText().toString().trim()
        val email: String = binding.etEmail.getText().toString().trim()
        val password: String = binding.etPassword.getText().toString().trim()
        val confirmPassword: String = binding.etConfirmPassword.getText().toString().trim()

        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            binding.etName.requestFocus()
            binding.btnSignUp.revertAnimation()
            return
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Valid email is required"
            binding.etEmail.requestFocus()
            binding.btnSignUp.revertAnimation()
            return
        }

        if (password.isEmpty() || password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            binding.etPassword.requestFocus()
            binding.btnSignUp.revertAnimation()
            return
        }

        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            binding.etConfirmPassword.requestFocus()
            binding.btnSignUp.revertAnimation()
            return
        }

        // Register User in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser: FirebaseUser? = mAuth.currentUser

                    if (firebaseUser != null) {
                        val userId = firebaseUser.uid
                        val userModel = UserModel(userId, name, email)

                        firestore.collection(USER_DATABASE).document(userId)
                            .set(userModel)
                            .addOnCompleteListener { dbTask: Task<Void?> ->
                                binding.btnSignUp.revertAnimation()

                                if (dbTask.isSuccessful) {
                                    requireActivity().onBackPressed()

                                    Toast.makeText(
                                        context,
                                        "Registered successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to save user data",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                } else {
                    binding.btnSignUp.revertAnimation()

                    Toast.makeText(
                        context,
                        "Registration failed: " + task.exception?.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}
