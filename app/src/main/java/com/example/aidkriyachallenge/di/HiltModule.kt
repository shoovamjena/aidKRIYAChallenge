package com.example.aidkriyachallenge.di

import android.content.Context
import com.example.aidkriyachallenge.common.UserPreferences
import com.example.aidkriyachallenge.repo.Repo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object HiltModule {


    @Provides
    @Singleton
    fun ProvideFireStoreInstance() = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun ProvideAuth() = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun ProvideFirebaseStorage() = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideRepo(firestore: FirebaseFirestore,auth: FirebaseAuth,storage: FirebaseStorage) : Repo {
        return Repo(firestore = firestore, auth = auth, storage = storage)

    }

    @Provides
    @Singleton
    fun provideUserPref(@ApplicationContext context: Context): UserPreferences{
        return UserPreferences(context = context)
    }


}