/*
 * Copyright 2014 serso aka se.solovyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Contact details
 *
 * Email: se.solovyev@gmail.com
 * Site:  http://se.solovyev.org
 */

package org.solovyev.android.checkout;

import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Class which contains information about products, SKUs and purchases. This class can't be
 * instantiated manually but only through {@link Checkout#loadInventory(Request, Callback)} or
 * {@link Checkout#makeInventory()} method calls.
 * Note that this class doesn't reflect a real-time billing info. It is not updated or notified if
 * an item is purchased or cancelled; its contents are static and updated only when
 * {@link #load(Request, Callback)} is called.
 * This class lifecycle is bound to the lifecycle of {@link Checkout} in which it was created. If
 * {@link Checkout} stops this class loading also stops and no
 * {@link Callback#onLoaded(Inventory.Products)} method is called.
 */
public interface Inventory {

    /**
     * Loads a list of SKUs and asynchronously delivers it to the provided {@link Callback}.
     * Multiple simultaneous loadings are not supported, each new call of this method cancels all
     * previous requests.
     * @param request list of SKUs to be loaded
     * @return instance of this {@link Inventory}
     */
    @Nonnull
    Inventory load(@Nonnull Request request, @Nonnull Callback callback);

    /**
     * Cancels current loading, if any.
     */
    void cancel();

    /**
     * Note that this method may return different instances of {@link Inventory.Products} with
     * different contents
     * @return the last loaded set of products
     */
    @Nonnull
    Inventory.Products getProducts();

    /**
     * A callback of {@link #load(Request, Callback)} method.
     */
    interface Callback {
        /**
         * Called when all the products were loaded. Note that this method is called even if the
         * loading fails.
         * @param products loaded products
         */
        void onLoaded(@Nonnull Inventory.Products products);
    }

    /**
     * Set of products in the inventory.
     */
    @Immutable
    final class Products implements Iterable<Inventory.Product> {

        @Nonnull
        static final Products EMPTY = new Products();

        @Nonnull
        private final Map<String, Inventory.Product> mMap = new HashMap<>();

        Products() {
            for (String product : ProductTypes.ALL) {
                mMap.put(product, new Product(product, false));
            }
        }

        void add(@Nonnull Inventory.Product product) {
            mMap.put(product.id, product);
        }

        /**
         * @param productId product id
         * @return product by id
         */
        @Nonnull
        public Inventory.Product get(@Nonnull String productId) {
            ProductTypes.checkSupported(productId);
            return mMap.get(productId);
        }

        /**
         * @return unmodifiable iterator which iterates over all products
         */
        @Override
        public Iterator<Inventory.Product> iterator() {
            return unmodifiableCollection(mMap.values()).iterator();
        }

        /**
         * @return number of products
         */
        public int size() {
            return mMap.size();
        }

        void merge(@Nonnull Products products) {
            for (Map.Entry<String, Product> entry : mMap.entrySet()) {
                if (!entry.getValue().supported) {
                    final Product product = products.mMap.get(entry.getKey());
                    if (product != null) {
                        entry.setValue(product);
                    }
                }
            }
        }
    }

    /**
     * One product in the inventory. Contains list of purchases and optionally list of SKUs (if
     * {@link Request} contains information about SKUs)
     */
    @Immutable
    final class Product {

        /**
         * Product ID, see {@link org.solovyev.android.checkout.ProductTypes}
         */
        @Nonnull
        public final String id;

        /**
         * True if product is supported by {@link Inventory}. Note that Billing for this product
         * might not be supported:
         * this just indicates that {@link Inventory} loaded purchases/SKUs for the product.
         */
        public final boolean supported;

        @Nonnull
        final List<Purchase> mPurchases = new ArrayList<>();

        @Nonnull
        final List<Sku> mSkus = new ArrayList<>();

        Product(@Nonnull String id, boolean supported) {
            ProductTypes.checkSupported(id);
            this.id = id;
            this.supported = supported;
        }

        public boolean isPurchased(@Nonnull Sku sku) {
            return isPurchased(sku.id.code);
        }

        public boolean isPurchased(@Nonnull String sku) {
            return hasPurchaseInState(sku, Purchase.State.PURCHASED);
        }

        public boolean hasPurchaseInState(@Nonnull String sku, @Nonnull Purchase.State state) {
            return getPurchaseInState(sku, state) != null;
        }

        @Nullable
        public Purchase getPurchaseInState(@Nonnull String sku, @Nonnull Purchase.State state) {
            return Purchases.getPurchaseInState(mPurchases, sku, state);
        }

        @Nullable
        public Purchase getPurchaseInState(@Nonnull Sku sku, @Nonnull Purchase.State state) {
            return getPurchaseInState(sku.id.code, state);
        }

        /**
         * This list doesn't contain duplicates, i.e. each element in the list has unique SKU
         * @return unmodifiable list of purchases sorted by purchase date (latest first)
         */
        @Nonnull
        public List<Purchase> getPurchases() {
            return unmodifiableList(mPurchases);
        }

        void setPurchases(@Nonnull List<Purchase> purchases) {
            Check.isTrue(mPurchases.isEmpty(), "Must be called only once");
            mPurchases.addAll(Purchases.neutralize(purchases));
            sort(mPurchases, PurchaseComparator.latestFirst());
        }

        /**
         * @return unmodifiable list of SKUs in the product
         */
        @Nonnull
        public List<Sku> getSkus() {
            return unmodifiableList(mSkus);
        }

        void setSkus(@Nonnull List<Sku> skus) {
            Check.isTrue(mSkus.isEmpty(), "Must be called only once");
            mSkus.addAll(skus);
        }
    }

    final class Request {
        // list of SKUs for which the details are loaded
        private final Map<String, List<String>> mSkus = new HashMap<>();
        // set of products for which purchase information is loaded
        private final Set<String> mProducts = new HashSet<>();

        private Request() {
            for (String product : ProductTypes.ALL) {
                mSkus.put(product, new ArrayList<String>(5));
            }
        }

        @Nonnull
        Request copy() {
            final Request copy = new Request();
            copy.mSkus.putAll(mSkus);
            copy.mProducts.addAll(mProducts);
            return copy;
        }

        @Nonnull
        public static Request create() {
            return new Request();
        }

        @Nonnull
        public Request loadAllPurchases() {
            mProducts.addAll(ProductTypes.ALL);
            return this;
        }

        @Nonnull
        public Request loadPurchases(@Nonnull String product) {
            ProductTypes.checkSupported(product);
            mProducts.add(product);
            return this;
        }

        boolean shouldLoadPurchases(@Nonnull String product) {
            return mProducts.contains(product);
        }

        @Nonnull
        public Request loadInAppSkus(@Nonnull List<String> skus) {
            loadSkus(ProductTypes.IN_APP, skus);
            return this;
        }

        @Nonnull
        public Request loadSubscriptionSkus(@Nonnull List<String> skus) {
            loadSkus(ProductTypes.SUBSCRIPTION, skus);
            return this;
        }

        @Nonnull
        public Request loadSkus(@Nonnull String product, @Nonnull List<String> skus) {
            for (String sku : skus) {
                loadSkus(product, sku);
            }
            return this;
        }

        @Nonnull
        public Request loadSkus(@Nonnull String product, @Nonnull String sku) {
            ProductTypes.checkSupported(product);
            Check.isNotEmpty(sku);

            final List<String> list = mSkus.get(product);
            Check.isTrue(!list.contains(sku), "Adding same SKU is not allowed");
            list.add(sku);
            return this;
        }

        boolean shouldLoadSkus(@Nonnull String product) {
            ProductTypes.checkSupported(product);
            return !mSkus.get(product).isEmpty();
        }

        @Nonnull
        public List<String> getSkus(@Nonnull String product) {
            return mSkus.get(product);
        }
    }
}
