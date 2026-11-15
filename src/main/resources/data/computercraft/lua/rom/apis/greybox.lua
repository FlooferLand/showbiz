local function report(...)
    local args = {...}
    local funcName = args[1]
    error("Stub called\nYou need to use peripheral.find(\"greybox\") then call "..tostring(funcName).." on the result!\nThe global greybox is just a stub for autocompletion", 2)
    return {}
end

---@param bits table
function setBits(bits)
    return report("setBits", bits)
end

---@param bitId number
---@return boolean
function hasBit(bitId)
    return report("hasBit", bitId)
end

---@param mapping string
function setMapping(mapping)
    report("setMapping", mapping)
end

---@return string[]
function getRegisteredMappings()
    return report("getRegisteredMappings")
end
