package com.example.mediaplayer.util.di

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.mediaplayer.view.adapter.FolderAdapter
import com.example.mediaplayer.view.adapter.SongAdapter
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.upstream.DefaultDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideGlideInstance(
        @ApplicationContext context: Context
    ) = Glide.with(context).setDefaultRequestOptions(RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.DATA))

    @Singleton
    @Provides
    fun provideAppContext(
        @ApplicationContext context: Context
    ) = context

    @Singleton
    @Provides
    fun provideAudioAttributes() = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    @Singleton
    @Provides
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes
    ) = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(audioAttributes, true)
        setHandleAudioBecomingNoisy(true)
    }

    @Singleton
    @Provides
    fun provideDataSourceFactory(
        @ApplicationContext context: Context
    ) = DefaultDataSource.Factory(context)

    @Singleton
    @Named("songAdapter")
    @Provides
    fun provideSongAdapter(
        @ApplicationContext context: Context,
        glide: RequestManager
    ) = SongAdapter(glide, context)

    @Named("songAdapterNS")
    @Provides
    fun provideSongAdapterNS(
        @ApplicationContext context: Context,
        glide: RequestManager
    ) = SongAdapter(glide, context)

    @Singleton
    @Named("folderAdapter")
    @Provides
    fun provideFolderAdapter(
        @ApplicationContext context: Context,
        glide: RequestManager
    ) = FolderAdapter(glide, context)

    @Named("folderAdapterNS")
    @Provides
    fun provideFolderAdapterNS(
        @ApplicationContext context: Context,
        glide: RequestManager
    ) = FolderAdapter(glide, context)

}