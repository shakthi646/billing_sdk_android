package com.billingsdk.ui.paywall;

import com.billingsdk.domain.BillingRepository;
import com.billingsdk.model.BillingSdkConfig;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class PaywallViewModel_Factory implements Factory<PaywallViewModel> {
  private final Provider<BillingRepository> repositoryProvider;

  private final Provider<BillingSdkConfig> configProvider;

  private PaywallViewModel_Factory(Provider<BillingRepository> repositoryProvider,
      Provider<BillingSdkConfig> configProvider) {
    this.repositoryProvider = repositoryProvider;
    this.configProvider = configProvider;
  }

  @Override
  public PaywallViewModel get() {
    return newInstance(repositoryProvider.get(), configProvider.get());
  }

  public static PaywallViewModel_Factory create(Provider<BillingRepository> repositoryProvider,
      Provider<BillingSdkConfig> configProvider) {
    return new PaywallViewModel_Factory(repositoryProvider, configProvider);
  }

  public static PaywallViewModel newInstance(BillingRepository repository,
      BillingSdkConfig config) {
    return new PaywallViewModel(repository, config);
  }
}
