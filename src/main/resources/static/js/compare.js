/**
 * NEXUSTECH FLAGSHIP STORE - Product Comparison Page Logic (/compare)
 * Clean, basic white design synchronized with other pages.
 */

let currentCompareCategoryId = null;
let currentCompareProduct1Id = null;
let currentCompareProduct2Id = null;
let currentCategoryProducts = [];

document.addEventListener('DOMContentLoaded', function () {
    initComparePage();
});

function initComparePage() {
    const activeBtn = document.querySelector('.cat-pill-btn.active');
    if (activeBtn) {
        currentCompareCategoryId = activeBtn.getAttribute('data-cat-id');
    }

    const select1 = document.getElementById('compareSelect1');
    const select2 = document.getElementById('compareSelect2');
    if (select1 && select2) {
        currentCompareProduct1Id = select1.value;
        currentCompareProduct2Id = select2.value;
    }
}

/**
 * Handle category tab change
 */
function switchCompareCategory(categoryId, btnEl) {
    if (currentCompareCategoryId == categoryId && currentCategoryProducts.length > 0) {
        return;
    }

    currentCompareCategoryId = categoryId;

    // Update active tab buttons
    document.querySelectorAll('.cat-pill-btn').forEach(b => b.classList.remove('active'));
    if (btnEl) {
        btnEl.classList.add('active');
    }

    hideCompareAlert();

    // Fetch products belonging to this category
    fetch('/api/compare/category/' + categoryId + '/products')
        .then(res => res.json())
        .then(products => {
            currentCategoryProducts = products;
            if (!products || products.length === 0) {
                showCompareAlert('Danh mục này hiện chưa có sản phẩm nào để so sánh.');
                renderEmptyCompare('Danh mục này chưa có sản phẩm.');
                return;
            }

            // Populate both dropdowns
            populateSelectOptions('compareSelect1', products, products[0]?.id);
            const secondId = products.length > 1 ? products[1].id : products[0].id;
            populateSelectOptions('compareSelect2', products, secondId);

            currentCompareProduct1Id = products[0]?.id;
            currentCompareProduct2Id = secondId;

            if (products.length >= 2) {
                loadComparisonData(currentCompareProduct1Id, currentCompareProduct2Id);
            } else {
                showCompareAlert('Danh mục này hiện chỉ có 1 sản phẩm. Cần ít nhất 2 sản phẩm cùng loại để đối chiếu.');
                loadComparisonData(currentCompareProduct1Id, currentCompareProduct1Id);
            }
        })
        .catch(err => {
            console.error('Lỗi khi tải danh sách sản phẩm cùng loại:', err);
            showCompareAlert('Không thể kết nối đến máy chủ để tải sản phẩm.');
        });
}

function populateSelectOptions(selectId, products, selectedId) {
    const select = document.getElementById(selectId);
    if (!select) return;

    select.innerHTML = '';
    products.forEach(p => {
        const opt = document.createElement('option');
        opt.value = p.id;
        opt.textContent = `${p.name} — ${p.formattedPrice || 'Liên hệ'}`;
        if (p.id == selectedId) {
            opt.selected = true;
        }
        select.appendChild(opt);
    });
}

/**
 * Handle product select change in Slot 1 or Slot 2
 */
function onCompareProductChange(slot, newProductId) {
    hideCompareAlert();

    if (slot === 1) {
        currentCompareProduct1Id = newProductId;
    } else {
        currentCompareProduct2Id = newProductId;
    }

    if (currentCompareProduct1Id == currentCompareProduct2Id) {
        showCompareAlert('Lưu ý: Bạn đang chọn cùng 1 điện thoại cho cả 2 vị trí. Hãy chọn 2 dòng điện thoại khác nhau để so sánh đối chiếu.');
    }

    loadComparisonData(currentCompareProduct1Id, currentCompareProduct2Id);
}

/**
 * Swap Slot 1 and Slot 2
 */
function swapCompareProducts() {
    if (!currentCompareProduct1Id || !currentCompareProduct2Id) return;

    const temp = currentCompareProduct1Id;
    currentCompareProduct1Id = currentCompareProduct2Id;
    currentCompareProduct2Id = temp;

    const s1 = document.getElementById('compareSelect1');
    const s2 = document.getElementById('compareSelect2');
    if (s1) s1.value = currentCompareProduct1Id;
    if (s2) s2.value = currentCompareProduct2Id;

    loadComparisonData(currentCompareProduct1Id, currentCompareProduct2Id);
}

/**
 * Load comparison data from backend API
 */
function loadComparisonData(id1, id2) {
    if (!id1 || !id2) return;

    fetch(`/api/compare/data?id1=${id1}&id2=${id2}`)
        .then(res => {
            if (!res.ok) {
                return res.json().then(json => { throw new Error(json.error || 'Lỗi so sánh sản phẩm'); });
            }
            return res.json();
        })
        .then(data => {
            hideCompareAlert();
            renderComparisonResult(data);
        })
        .catch(err => {
            console.error('Lỗi đối chiếu sản phẩm:', err);
            showCompareAlert(err.message || 'Không thể so sánh 2 sản phẩm đã chọn.');
        });
}

/**
 * Render comparison data onto DOM
 */
function renderComparisonResult(data) {
    const p1 = data.product1;
    const p2 = data.product2;

    const grid = document.getElementById('compareOverviewGrid');
    const tableBody = document.getElementById('compareTableBody');
    if (!grid || !tableBody) return;

    // 1. Render Top Product Boxes
    grid.innerHTML = `
        <!-- Product 1 Card -->
        <div class="compare-prod-box">
            <div class="compare-img-wrap">
                <img src="${p1.imageUrl}" alt="${escapeHtml(p1.name)}" class="compare-prod-img">
            </div>
            <div class="compare-prod-info">
                <span class="compare-brand-tag">${escapeHtml(p1.brandName || 'Chính Hãng')}</span>
                <h2 class="compare-prod-name">
                    <a href="/product/${p1.slug}">${escapeHtml(p1.name)}</a>
                </h2>
                <div class="compare-price-box">
                    <span class="price-current">${p1.formattedPrice}</span>
                    ${p1.formattedOriginalPrice ? `<span class="price-old">${p1.formattedOriginalPrice}</span>` : ''}
                </div>
                <p class="compare-stock-text ${p1.inStock ? 'in-stock' : 'out-stock'}">
                    ${p1.inStock ? 'Còn hàng' : 'Tạm hết hàng'}
                </p>
                <a href="/product/${p1.slug}" class="btn-view-prod">Xem chi tiết</a>
            </div>
        </div>

        <!-- Product 2 Card -->
        <div class="compare-prod-box">
            <div class="compare-img-wrap">
                <img src="${p2.imageUrl}" alt="${escapeHtml(p2.name)}" class="compare-prod-img">
            </div>
            <div class="compare-prod-info">
                <span class="compare-brand-tag">${escapeHtml(p2.brandName || 'Chính Hãng')}</span>
                <h2 class="compare-prod-name">
                    <a href="/product/${p2.slug}">${escapeHtml(p2.name)}</a>
                </h2>
                <div class="compare-price-box">
                    <span class="price-current">${p2.formattedPrice}</span>
                    ${p2.formattedOriginalPrice ? `<span class="price-old">${p2.formattedOriginalPrice}</span>` : ''}
                </div>
                <p class="compare-stock-text ${p2.inStock ? 'in-stock' : 'out-stock'}">
                    ${p2.inStock ? 'Còn hàng' : 'Tạm hết hàng'}
                </p>
                <a href="/product/${p2.slug}" class="btn-view-prod">Xem chi tiết</a>
            </div>
        </div>
    `;

    // 2. Render Specs Matrix Rows
    const screenWinner1 = (data.screenSizeWinner === 1) ? `<span class="winner-pill">Lớn hơn</span>` : '';
    const screenWinner2 = (data.screenSizeWinner === 2) ? `<span class="winner-pill">Lớn hơn</span>` : '';

    const batteryWinner1 = (data.batteryWinner === 1) ? `<span class="winner-pill">Pin lớn hơn</span>` : '';
    const batteryWinner2 = (data.batteryWinner === 2) ? `<span class="winner-pill">Pin lớn hơn</span>` : '';

    const priceWinner1 = (data.priceWinner === 1) ? `<span class="winner-pill">Giá tốt hơn</span>` : '';
    const priceWinner2 = (data.priceWinner === 2) ? `<span class="winner-pill">Giá tốt hơn</span>` : '';

    const rams1 = renderPills(p1.ramOptions, 'Chưa cập nhật');
    const rams2 = renderPills(p2.ramOptions, 'Chưa cập nhật');

    const storages1 = renderPills(p1.storageOptions, 'Chưa cập nhật');
    const storages2 = renderPills(p2.storageOptions, 'Chưa cập nhật');

    const colors1 = renderPills(p1.colorOptions, 'Đa dạng màu');
    const colors2 = renderPills(p2.colorOptions, 'Đa dạng màu');

    tableBody.innerHTML = `
        <tr>
            <td class="col-spec-label">Kích thước màn hình</td>
            <td class="col-spec-val border-right">
                <strong>${escapeHtml(p1.formattedScreenSize || 'Đang cập nhật')}</strong>
                ${screenWinner1}
            </td>
            <td class="col-spec-val">
                <strong>${escapeHtml(p2.formattedScreenSize || 'Đang cập nhật')}</strong>
                ${screenWinner2}
            </td>
        </tr>
        <tr>
            <td class="col-spec-label">Vi xử lý / Chipset</td>
            <td class="col-spec-val border-right">${escapeHtml(p1.chipset || 'Đang cập nhật')}</td>
            <td class="col-spec-val">${escapeHtml(p2.chipset || 'Đang cập nhật')}</td>
        </tr>
        <tr>
            <td class="col-spec-label">Dung lượng Pin</td>
            <td class="col-spec-val border-right">
                <strong>${escapeHtml(p1.formattedBattery || 'Đang cập nhật')}</strong>
                ${batteryWinner1}
            </td>
            <td class="col-spec-val">
                <strong>${escapeHtml(p2.formattedBattery || 'Đang cập nhật')}</strong>
                ${batteryWinner2}
            </td>
        </tr>
        <tr>
            <td class="col-spec-label">Hệ điều hành</td>
            <td class="col-spec-val border-right">${escapeHtml(p1.operatingSystem || 'Tiêu chuẩn')}</td>
            <td class="col-spec-val">${escapeHtml(p2.operatingSystem || 'Tiêu chuẩn')}</td>
        </tr>
        <tr>
            <td class="col-spec-label">Tùy chọn RAM</td>
            <td class="col-spec-val border-right">${rams1}</td>
            <td class="col-spec-val">${rams2}</td>
        </tr>
        <tr>
            <td class="col-spec-label">Bộ nhớ trong (ROM)</td>
            <td class="col-spec-val border-right">${storages1}</td>
            <td class="col-spec-val">${storages2}</td>
        </tr>
        <tr>
            <td class="col-spec-label">Tùy chọn màu sắc</td>
            <td class="col-spec-val border-right">${colors1}</td>
            <td class="col-spec-val">${colors2}</td>
        </tr>
        <tr>
            <td class="col-spec-label">Giá bán</td>
            <td class="col-spec-val border-right">
                <span class="price-val">${p1.formattedPrice}</span>
                ${priceWinner1}
            </td>
            <td class="col-spec-val">
                <span class="price-val">${p2.formattedPrice}</span>
                ${priceWinner2}
            </td>
        </tr>
    `;
}

function renderPills(options, fallback) {
    if (!options || options.length === 0) {
        return `<span style="color: #64748b; font-size: 13px;">${fallback}</span>`;
    }
    return `
        <div class="clean-pill-group">
            ${options.map(o => `<span class="clean-pill">${escapeHtml(o)}</span>`).join('')}
        </div>
    `;
}

function renderEmptyCompare(msg) {
    const grid = document.getElementById('compareOverviewGrid');
    const tableBody = document.getElementById('compareTableBody');
    if (grid) {
        grid.innerHTML = `
            <div style="grid-column: 1 / -1; text-align: center; padding: 40px; color: #64748b;">
                <p style="font-size: 15px;">${escapeHtml(msg)}</p>
            </div>
        `;
    }
    if (tableBody) {
        tableBody.innerHTML = '';
    }
}

function showCompareAlert(msg) {
    const alert = document.getElementById('compareAlert');
    if (alert) {
        alert.textContent = msg;
        alert.style.display = 'block';
    }
}

function hideCompareAlert() {
    const alert = document.getElementById('compareAlert');
    if (alert) {
        alert.style.display = 'none';
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
