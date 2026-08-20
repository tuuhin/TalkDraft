package com.sam.talkdraft.supabase

import io.github.jan.supabase.SupabaseClient

fun interface ISupabaseProvider {

	fun providesSupabase(): SupabaseClient
}