package com.billingsdk.di;

import com.billingsdk.data.BillingRepositoryImpl;
import com.billingsdk.domain.BillingRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
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
public final class BillingRepositoryBinding_ProvideBillingRepositoryFactory implements Factory<BillingRepository> {
  private final Provider<BillingRepositoryImpl> implProvider;

  private BillingRepositoryBinding_ProvideBillingRepositoryFactory(
      Provider<BillingRepositoryImpl> implProvider) {
    this.implProvider = implProvider;
  }

  @Override
  public BillingRepository get() {
    return provideBillingRepository(implProvider.get());
  }

  public static BillingRepositoryBinding_ProvideBillingRepositoryFactory create(
      Provider<BillingRepositoryImpl> implProvider) {
    return new BillingRepositoryBinding_ProvideBillingRepositoryFactory(implProvider);
  }

  public static BillingRepository provideBillingRepository(BillingRepositoryImpl impl) {
    return Preconditions.checkNotNullFromProvides(BillingRepositoryBinding.INSTANCE.provideBillingRepository(impl));
  }
}
