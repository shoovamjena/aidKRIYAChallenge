package com.example.aidkriyachallenge.di

import com.example.aidkriyachallenge.repo.repo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun provideRepo(firestore: FirebaseFirestore,auth: FirebaseAuth) : repo {
        return repo(firestore = firestore, auth = auth)

    }


}