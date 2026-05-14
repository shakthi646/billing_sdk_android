package com.billingsdk.billing;

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
public final class BillingClientManager_Factory implements Factory<BillingClientManager> {
  private final Provider<Context> appContextProvider;

  private BillingClientManager_Factory(Provider<Context> appContextProvider) {
    this.appContextProvider = appContextProvider;
  }

  @Override
  public BillingClientManager get() {
    return newInstance(appContextProvider.get());
  }

  public static BillingClientManager_Factory create(Provider<Context> appContextProvider) {
    return new BillingClientManager_Factory(appContextProvider);
  }

  public static BillingClientManager newInstance(Context appContext) {
    return new BillingClientManager(appContext);
  }
}
