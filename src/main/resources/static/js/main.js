/**
 * NEXUSTECH FLAGSHIP STORE - FRONTEND INTERACTION LOGIC
 * Pure Vanilla JavaScript - No AJAX / No REST API
 */

document.addEventListener('DOMContentLoaded', function () {
    initCategoryTabs();
    initOtpInputs();
    initOtpTimer();
    initPasswordToggles();
});

/**
 * 1. Filter Category Pills (Instant Client-side switching without AJAX)
 */
function initCategoryTabs() {
    const filterPills = document.querySelectorAll('.filter-pill[data-filter]');
    const productCards = document.querySelectorAll('.product-card[data-brand]');

    if (!filterPills.length || !productCards.length) return;

    filterPills.forEach(pill => {
        pill.addEventListener('click', function (e) {
            // Prevent default jump
            e.preventDefault();

            // Toggle active pill
            filterPills.forEach(p => p.classList.remove('active'));
            this.classList.add('active');

            const filterValue = this.getAttribute('data-filter').toLowerCase();

            productCards.forEach(card => {
                const cardBrand = (card.getAttribute('data-brand') || '').toLowerCase();
                const cardCategory = (card.getAttribute('data-category') || '').toLowerCase();

                if (filterValue === 'all') {
                    card.style.display = 'flex';
                } else if (filterValue === 'phu-kien') {
                    card.style.display = (cardCategory.includes('phụ kiện') || cardCategory.includes('phu-kien')) ? 'flex' : 'none';
                } else if (cardBrand.includes(filterValue)) {
                    card.style.display = 'flex';
                } else {
                    card.style.display = 'none';
                }
            });
        });
    });
}

/**
 * 2. OTP 6-Digit Auto Advance & Paste Handling
 */
function initOtpInputs() {
    const otpContainer = document.querySelector('.otp-boxes-group');
    if (!otpContainer) return;

    const inputs = otpContainer.querySelectorAll('.otp-digit-input');
    const hiddenOtpInput = document.getElementById('hiddenOtp');
    const otpForm = document.getElementById('otpForm');

    function syncHiddenInput() {
        let code = '';
        inputs.forEach(input => {
            code += input.value.trim();
        });
        if (hiddenOtpInput) {
            hiddenOtpInput.value = code;
        }
    }

    inputs.forEach((input, index) => {
        // Only allow numbers
        input.addEventListener('input', function (e) {
            const val = this.value.replace(/[^0-9]/g, '');
            this.value = val ? val[val.length - 1] : '';

            syncHiddenInput();

            if (this.value && index < inputs.length - 1) {
                inputs[index + 1].focus();
                inputs[index + 1].select();
            }
        });

        // Handle Backspace navigation
        input.addEventListener('keydown', function (e) {
            if (e.key === 'Backspace' && !this.value && index > 0) {
                inputs[index - 1].focus();
                inputs[index - 1].select();
            }
        });

        // Handle Paste (e.g. user copies '482910')
        input.addEventListener('paste', function (e) {
            e.preventDefault();
            const pasteData = (e.clipboardData || window.clipboardData).getData('text').replace(/[^0-9]/g, '');
            if (pasteData.length > 0) {
                const digits = pasteData.slice(0, inputs.length).split('');
                digits.forEach((digit, i) => {
                    if (inputs[i]) inputs[i].value = digit;
                });
                syncHiddenInput();

                const nextFocus = Math.min(digits.length, inputs.length - 1);
                inputs[nextFocus].focus();
            }
        });
    });

    if (otpForm) {
        otpForm.addEventListener('submit', function (e) {
            syncHiddenInput();
            if (!hiddenOtpInput || hiddenOtpInput.value.length !== 6) {
                e.preventDefault();
                alert('Vui lòng nhập đủ 6 chữ số mã OTP!');
            }
        });
    }

    // Auto focus first input
    if (inputs[0]) {
        inputs[0].focus();
    }
}

/**
 * 3. OTP 5-Minute Countdown Timer
 */
function initOtpTimer() {
    const timerDisplay = document.getElementById('otpCountdown');
    const resendBtn = document.getElementById('resendOtpBtn');
    if (!timerDisplay) return;

    let totalSeconds = 300; // 5 minutes

    const interval = setInterval(function () {
        totalSeconds--;

        if (totalSeconds <= 0) {
            clearInterval(interval);
            timerDisplay.textContent = '00:00 (Hết hạn)';
            timerDisplay.style.color = '#f87171';
            if (resendBtn) {
                resendBtn.removeAttribute('disabled');
            }
            return;
        }

        const minutes = Math.floor(totalSeconds / 60);
        const seconds = totalSeconds % 60;
        timerDisplay.textContent = 
            (minutes < 10 ? '0' : '') + minutes + ':' + 
            (seconds < 10 ? '0' : '') + seconds;

        // Allow resend after 45 seconds have passed
        if (totalSeconds <= 255 && resendBtn && resendBtn.hasAttribute('disabled')) {
            resendBtn.removeAttribute('disabled');
        }
    }, 1000);
}

/**
 * 4. Password Visibility Toggle
 */
function initPasswordToggles() {
    const toggleButtons = document.querySelectorAll('.password-toggle-btn');
    toggleButtons.forEach(btn => {
        btn.addEventListener('click', function () {
            const targetId = this.getAttribute('data-target');
            const targetInput = document.getElementById(targetId);
            if (!targetInput) return;

            if (targetInput.type === 'password') {
                targetInput.type = 'text';
                this.innerHTML = '👁️';
            } else {
                targetInput.type = 'password';
                this.innerHTML = '🔒';
            }
        });
    });
}

/**
 * Helper to auto-fill VIP demo credentials on login screen
 */
function fillDemoAccount(email, password) {
    const emailField = document.getElementById('loginEmail');
    const passField = document.getElementById('loginPassword');
    if (emailField && passField) {
        emailField.value = email;
        passField.value = password;
    }
}

/**
 * 5-Role quick selector handler
 */
function selectDemoRole(email, password, roleCode, roleTitle) {
    fillDemoAccount(email, password);

    // Highlight selected button
    const buttons = document.querySelectorAll('.demo-role-btn');
    buttons.forEach(btn => btn.classList.remove('active'));

    const activeBtn = document.querySelector(`.role-btn-${roleCode.toLowerCase()}`);
    if (activeBtn) {
        activeBtn.classList.add('active');
    }

    // Show toast banner
    const banner = document.getElementById('demoSelectedBanner');
    const badge = document.getElementById('selectedRoleBadge');
    const emailSpan = document.getElementById('selectedRoleEmail');

    if (banner && badge && emailSpan) {
        banner.style.display = 'flex';
        badge.textContent = roleCode;
        badge.className = `selected-pill badge-role-${roleCode.toLowerCase()}`;
        emailSpan.textContent = `${email} • ${roleTitle}`;
    }
}
