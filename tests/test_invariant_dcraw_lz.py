import pytest
import ctypes
import struct


# Simulate the buffer copy behavior that mirrors the vulnerable C code pattern
# We implement a Python model of the fixed-size buffer copy to test the invariant

MAKE_BUFFER_SIZE = 64
MODEL_BUFFER_SIZE = 64


def safe_strcpy_to_fixed_buffer(dest_size: int, source: str) -> bytes:
    """
    Simulates what a SAFE implementation should do:
    Copy source string into a fixed-size buffer, ensuring no overflow.
    Returns the buffer contents (truncated if necessary).
    """
    encoded = source.encode('utf-8', errors='replace')
    # Safe behavior: truncate to dest_size - 1 (leave room for null terminator)
    safe_length = dest_size - 1
    truncated = encoded[:safe_length]
    # Pad with null bytes to fill buffer
    result = truncated + b'\x00' * (dest_size - len(truncated))
    return result


def unsafe_strcpy_model(dest_size: int, source: str) -> int:
    """
    Models the unsafe strcpy behavior.
    Returns the number of bytes that WOULD be written (including null terminator).
    If this exceeds dest_size, it indicates a buffer overflow.
    """
    encoded = source.encode('utf-8', errors='replace')
    # strcpy copies all bytes including null terminator
    bytes_written = len(encoded) + 1  # +1 for null terminator
    return bytes_written


def validate_buffer_copy(dest_size: int, source: str) -> dict:
    """
    Validates whether copying source into a fixed buffer of dest_size is safe.
    Returns a dict with:
      - 'overflow': bool indicating if overflow would occur
      - 'bytes_needed': how many bytes would be written
      - 'buffer_size': the declared buffer size
      - 'safe_result': what a safe implementation would produce
    """
    bytes_needed = unsafe_strcpy_model(dest_size, source)
    overflow = bytes_needed > dest_size
    safe_result = safe_strcpy_to_fixed_buffer(dest_size, source)
    
    return {
        'overflow': overflow,
        'bytes_needed': bytes_needed,
        'buffer_size': dest_size,
        'safe_result': safe_result,
        'safe_result_length': len(safe_result),
    }


# Attack payloads: (make_string, model_string, description)
ATTACK_PAYLOADS = [
    # 2x buffer size overflow attempts
    ("A" * 128, "B" * 128, "2x buffer size - all same char"),
    # 10x buffer size overflow attempts
    ("A" * 640, "B" * 640, "10x buffer size - all same char"),
    # Exactly at boundary (should be safe)
    ("A" * 63, "B" * 63, "exactly buffer_size - 1 (safe boundary)"),
    # One byte over boundary
    ("A" * 64, "B" * 64, "exactly buffer_size (off-by-one overflow)"),
    # One byte over with null terminator consideration
    ("A" * 65, "B" * 65, "buffer_size + 1 (overflow)"),
    # Realistic camera make/model with overflow
    ("Canon EOS " + "X" * 100, "EOS Rebel " + "Y" * 100, "realistic prefix with overflow suffix"),
    # Null bytes embedded (C string termination bypass attempt)
    ("Nikon\x00" + "A" * 100, "D850\x00" + "B" * 100, "embedded null bytes with overflow"),
    # Unicode/multibyte characters that expand when encoded
    ("\u00e9" * 64, "\u00e9" * 64, "latin extended chars 2-byte UTF-8 encoding"),
    # Very long strings
    ("Sony" + "A" * 1000, "Alpha" + "B" * 1000, "very long strings 1000+ chars"),
    # Format string attack patterns
    ("%s%s%s%s%s%s%s%s%s%s" * 10, "%n%n%n%n%n%n%n%n%n%n" * 10, "format string patterns"),
    # Path traversal in make/model
    ("../../../etc/passwd" * 5, "../../../../etc/shadow" * 5, "path traversal patterns"),
    # Special characters
    ("\xff\xfe" * 64, "\x00\xff" * 64, "high byte values"),
    # Empty strings (edge case - should always be safe)
    ("", "", "empty strings"),
    # Single character (safe)
    ("A", "B", "single character strings"),
    # Exactly 64 chars (overflow - needs 65 bytes with null terminator)
    ("C" * 64, "D" * 64, "exactly 64 chars needs 65 bytes with null"),
    # Mixed content overflow
    ("Canon EOS 5D Mark IV Pro Edition Special" + "X" * 50,
     "EOS 5D Mark IV Professional Camera Body" + "Y" * 50,
     "mixed realistic and overflow content"),
    # Whitespace padding attack
    (" " * 200, "\t" * 200, "whitespace overflow"),
    # Newline injection
    ("Make\nInjected\n" * 10, "Model\rInjected\r" * 10, "newline injection overflow"),
]


@pytest.mark.parametrize("make_payload,model_payload,description", ATTACK_PAYLOADS)
def test_buffer_reads_never_exceed_declared_length(make_payload, model_payload, description):
    """
    Invariant: Buffer reads/writes must never exceed the declared buffer size.
    
    When copying camera make and model strings into fixed-size buffers (64 bytes),
    the resulting buffer must:
    1. Never exceed MAKE_BUFFER_SIZE or MODEL_BUFFER_SIZE bytes
    2. Always be null-terminated within the buffer bounds
    3. The safe copy result must fit within the declared buffer size
    
    This guards against CWE-120 buffer overflow vulnerabilities similar to
    the strcpy usage in dcraw_lz.c where make/model strings from a table
    are copied into fixed 64-byte buffers without bounds checking.
    """
    # Validate make buffer
    make_result = validate_buffer_copy(MAKE_BUFFER_SIZE, make_payload)
    model_result = validate_buffer_copy(MODEL_BUFFER_SIZE, model_payload)
    
    # INVARIANT 1: Safe copy result must always fit within declared buffer size
    assert make_result['safe_result_length'] == MAKE_BUFFER_SIZE, (
        f"[{description}] Make safe copy result length {make_result['safe_result_length']} "
        f"!= declared buffer size {MAKE_BUFFER_SIZE}"
    )
    assert model_result['safe_result_length'] == MODEL_BUFFER_SIZE, (
        f"[{description}] Model safe copy result length {model_result['safe_result_length']} "
        f"!= declared buffer size {MODEL_BUFFER_SIZE}"
    )
    
    # INVARIANT 2: Safe copy result must be null-terminated within buffer bounds
    assert b'\x00' in make_result['safe_result'], (
        f"[{description}] Make buffer has no null terminator"
    )
    assert b'\x00' in model_result['safe_result'], (
        f"[{description}] Model buffer has no null terminator"
    )
    
    # INVARIANT 3: The null terminator must appear within the buffer bounds
    make_null_pos = make_result['safe_result'].find(b'\x00')
    model_null_pos = model_result['safe_result'].find(b'\x00')
    
    assert make_null_pos < MAKE_BUFFER_SIZE, (
        f"[{description}] Make null terminator at position {make_null_pos} "
        f">= buffer size {MAKE_BUFFER_SIZE}"
    )
    assert model_null_pos < MODEL_BUFFER_SIZE, (
        f"[{description}] Model null terminator at position {model_null_pos} "
        f">= buffer size {MODEL_BUFFER_SIZE}"
    )
    
    # INVARIANT 4: If overflow would occur with unsafe copy, document it and
    # verify that a safe implementation would NOT overflow
    if make_result['overflow']:
        # The unsafe copy WOULD overflow - verify safe copy prevents it
        safe_make_content = make_result['safe_result'][:make_null_pos]
        assert len(safe_make_content) < MAKE_BUFFER_SIZE, (
            f"[{description}] Safe make content length {len(safe_make_content)} "
            f">= buffer size {MAKE_BUFFER_SIZE} - safe copy failed to prevent overflow"
        )
    
    if model_result['overflow']:
        # The unsafe copy WOULD overflow - verify safe copy prevents it
        safe_model_content = model_result['safe_result'][:model_null_pos]
        assert len(safe_model_content) < MODEL_BUFFER_SIZE, (
            f"[{description}] Safe model content length {len(safe_model_content)} "
            f">= buffer size {MODEL_BUFFER_SIZE} - safe copy failed to prevent overflow"
        )
    
    # INVARIANT 5: The actual string content in the buffer must not exceed
    # buffer_size - 1 bytes (leaving room for null terminator)
    make_string_in_buffer = make_result['safe_result'].split(b'\x00')[0]
    model_string_in_buffer = model_result['safe_result'].split(b'\x00')[0]
    
    assert len(make_string_in_buffer) <= MAKE_BUFFER_SIZE - 1, (
        f"[{description}] Make string in buffer ({len(make_string_in_buffer)} bytes) "
        f"exceeds max allowed ({MAKE_BUFFER_SIZE - 1} bytes)"
    )
    assert len(model_string_in_buffer) <= MODEL_BUFFER_SIZE - 1, (
        f"[{description}] Model string in buffer ({len(model_string_in_buffer)} bytes) "
        f"exceeds max allowed ({MODEL_BUFFER_SIZE - 1} bytes)"
    )


@pytest.mark.parametrize("buffer_size,payload", [
    (64, "A" * 63),    # safe: exactly fits
    (64, "A" * 64),    # unsafe: overflow by 1 (null terminator)
    (64, "A" * 65),    # unsafe: overflow by 2
    (64, "A" * 128),   # unsafe: 2x overflow
    (64, "A" * 640),   # unsafe: 10x overflow
    (64, ""),          # safe: empty string
    (64, "A"),         # safe: single char
])
def test_overflow_detection_accuracy(buffer_size, payload):
    """
    Invariant: Overflow detection must correctly identify when a strcpy would
    write beyond the declared buffer boundary.
    
    A buffer of size N can safely hold at most N-1 characters plus a null terminator.
    Any source string of length >= N will cause strcpy to write beyond the buffer.
    """
    result = validate_buffer_copy(buffer_size, payload)
    
    source_bytes = payload.encode('utf-8', errors='replace')
    expected_overflow = (len(source_bytes) + 1) > buffer_size  # +1 for null terminator
    
    assert result['overflow'] == expected_overflow, (
        f"Overflow detection mismatch for payload of length {len(payload)}: "
        f"expected overflow={expected_overflow}, got overflow={result['overflow']}"
    )
    
    # Regardless of overflow, safe copy must always fit in buffer
    assert result['safe_result_length'] == buffer_size, (
        f"Safe copy result must always equal buffer size {buffer_size}, "
        f"got {result['safe_result_length']}"
    )
    
    # Safe copy must always be null-terminated
    assert b'\x00' in result['safe_result'], (
        f"Safe copy result must always contain null terminator"
    )