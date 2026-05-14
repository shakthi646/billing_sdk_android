package com.billingsdk.di;

import com.billingsdk.model.BillingSdkConfig;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class BillingSdkConfigModule_ProvideBillingSdkConfigFactory implements Factory<BillingSdkConfig> {
  @Override
  public BillingSdkConfig get() {
    return provideBillingSdkConfig();
  }

  public static BillingSdkConfigModule_ProvideBillingSdkConfigFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static BillingSdkConfig provideBillingSdkConfig() {
    return Preconditions.checkNotNullFromProvides(BillingSdkConfigModule.INSTANCE.provideBillingSdkConfig());
  }

  private static final class InstanceHolder {
    static final BillingSdkConfigModule_ProvideBillingSdkConfigFactory INSTANCE = new BillingSdkConfigModule_ProvideBillingSdkConfigFactory();
  }
}
