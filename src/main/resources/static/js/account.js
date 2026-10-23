document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('addressForm');
    if (!form) return;

    const provinceSelect = document.getElementById('provinceSelect');
    const districtSelect = document.getElementById('districtSelect');
    const wardSelect = document.getElementById('wardSelect');
    const provinceName = document.getElementById('province');
    const districtName = document.getElementById('district');
    const wardName = document.getElementById('ward');
    const error = document.getElementById('locationError');
    const selectedProvince = provinceSelect.dataset.selected || '';
    const selectedDistrict = districtSelect.dataset.selected || '';
    const selectedWard = wardSelect.dataset.selected || '';

    function setOptions(select, options, placeholder, selectedCode) {
        select.replaceChildren(new Option(placeholder, ''));
        options.forEach(item => {
            const option = new Option(item.name, item.code);
            option.selected = String(item.code) === String(selectedCode);
            select.add(option);
        });
        select.disabled = options.length === 0;
    }

    async function fetchLocations(path) {
        const response = await fetch(path, { headers: { Accept: 'application/json' } });
        if (!response.ok) throw new Error('Không thể tải dữ liệu địa giới hành chính.');
        return response.json();
    }

    async function loadDistricts(provinceId, selectedId = '') {
        const districts = await fetchLocations(`/api/locations/provinces/${encodeURIComponent(provinceId)}/districts`);
        if (provinceSelect.value !== String(provinceId)) return;
        setOptions(districtSelect, districts, 'Chọn quận/huyện', selectedId);
        districtName.value = districtSelect.selectedOptions[0]?.textContent || '';
    }

    async function loadWards(districtId, selectedCode = '') {
        const wards = await fetchLocations(`/api/locations/districts/${encodeURIComponent(districtId)}/wards`);
        if (districtSelect.value !== String(districtId)) return;
        setOptions(wardSelect, wards, 'Chọn phường/xã', selectedCode);
        wardName.value = wardSelect.selectedOptions[0]?.textContent || '';
    }

    provinceSelect.addEventListener('change', async () => {
        provinceName.value = provinceSelect.selectedOptions[0]?.textContent || '';
        districtName.value = '';
        wardName.value = '';
        setOptions(districtSelect, [], provinceSelect.value ? 'Đang tải quận/huyện...' : 'Chọn quận/huyện', '');
        setOptions(wardSelect, [], 'Chọn phường/xã', '');
        if (!provinceSelect.value) return;
        const provinceId = provinceSelect.value;
        try {
            await loadDistricts(provinceId);
            if (provinceSelect.value !== provinceId) return;
            error.textContent = '';
        } catch (e) {
            if (provinceSelect.value !== provinceId) return;
            error.textContent = e.message;
            districtSelect.disabled = true;
        }
    });

    districtSelect.addEventListener('change', async () => {
        districtName.value = districtSelect.selectedOptions[0]?.textContent || '';
        wardName.value = '';
        setOptions(wardSelect, [], districtSelect.value ? 'Đang tải phường/xã...' : 'Chọn phường/xã', '');
        if (!districtSelect.value) return;
        const districtId = districtSelect.value;
        try {
            await loadWards(districtId);
            if (districtSelect.value !== districtId) return;
            error.textContent = '';
        } catch (e) {
            if (districtSelect.value !== districtId) return;
            error.textContent = e.message;
            wardSelect.disabled = true;
        }
    });

    wardSelect.addEventListener('change', () => {
        wardName.value = wardSelect.selectedOptions[0]?.textContent || '';
    });

    async function initialize() {
        try {
            const provinces = await fetchLocations('/api/locations/provinces');
            setOptions(provinceSelect, provinces, 'Chọn tỉnh/thành phố', selectedProvince);
            provinceName.value = provinceSelect.selectedOptions[0]?.textContent || '';
            if (!provinceSelect.value) return;
            await loadDistricts(provinceSelect.value, selectedDistrict);
            if (!districtSelect.value) return;
            await loadWards(districtSelect.value, selectedWard);
        } catch (e) {
            error.textContent = e.message;
            provinceSelect.disabled = true;
        }
    }
    initialize();
});
