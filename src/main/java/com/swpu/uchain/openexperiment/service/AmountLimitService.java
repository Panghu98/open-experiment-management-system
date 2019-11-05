package com.swpu.uchain.openexperiment.service;

import com.swpu.uchain.openexperiment.form.amount.AmountLimitForm;
import com.swpu.uchain.openexperiment.form.amount.AmountSearchForm;
import com.swpu.uchain.openexperiment.result.Result;

public interface AmountLimitService {

    Result getAmountByCollegeAndUnit(AmountSearchForm form);

    Result setAmount(AmountLimitForm form);
}