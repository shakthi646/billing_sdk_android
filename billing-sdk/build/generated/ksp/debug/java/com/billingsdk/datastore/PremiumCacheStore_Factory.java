package com.billingsdk.datastore;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class PremiumCacheStore_Factory implements Factory<PremiumCacheStore> {
  private final Provider<Context> contextProvider;

  private PremiumCacheStore_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public PremiumCacheStore get() {
    return newInstance(contextProvider.get());
  }

  public static PremiumCacheStore_Factory create(Provider<Context> contextProvider) {
    return new PremiumCacheStore_Factory(contextProvider);
  }

  public static PremiumCacheStore newInstance(Context context) {
    return new PremiumCacheStore(context);
  }
}
